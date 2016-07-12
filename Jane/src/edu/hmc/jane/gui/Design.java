package edu.hmc.jane.gui;

import components.TableSorter;
import beetree.*;
import com.apple.eawt.Application;
import edu.hmc.jane.*;
import edu.hmc.jane.io.*;
import edu.hmc.jane.solving.*;
import edu.hmc.jane.solving.LowerBound.*;
import edu.hmc.jane.util.DaemonThreadFactory;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;

/*
 * Design.java
 *
 * Created on Jul 20, 2010, 6:29:36 PM Modified on June 22, 2012, 11:29:00 AM
 */

/**
 *
 * @author Tselil Modified by Ki Wan Gkoo
 */
public class Design extends javax.swing.JFrame implements Thread.UncaughtExceptionHandler {

    // DATA MEMBERS
    boolean running = false;
    boolean fileLoaded = false;
    boolean solveMode = true; //don't change this back to false; it works
    boolean computed = false;
    boolean stop_now = false;
    boolean estimate = false;
    double memoryPerViewer;
    ProblemInstance prob;
    String filename;
    private Stats.HistoData currentStats = null;
    double singleRunSeconds = -1;
    List<EventSolver> currentSolutions = new Vector<EventSolver>();
    protected static SupportPopulation supportPop = new SupportPopulation();
    SolutionTableModel solutionModel;
    TableSorter sorter; //this is not the TableSorter class introduced in Java 6
    protected static JDialog keyDialog;
    protected static boolean keyOpen;
    // Editing Panels for costs etc.; set invisible until we need to use them.
    private EditCosts costEditor = new EditCosts(this);
    private SetGeneticParams geneticEditor = new SetGeneticParams(this);
    private SolutionsPerRun numSolutionsEditor = new SolutionsPerRun(this);
    private MaximumHostSwitchDistance distance = new MaximumHostSwitchDistance(this);
    private SetPolytomyParams polytomyParams = new SetPolytomyParams(this);
    private SetSupportParams supportParams = new SetSupportParams(this);
    private Thread blink;
    private Thread progress;
    private Thread statsThread;
    private Thread solveThread;
    // MENUS AND FILE FILTERS
    JFileChooser chooseTree;
    JFileChooser chooseTiming;
    JFileChooser sampleChooser;
    // last directory saved to by one of the save dialogs or opened from
    // by one of the open dialogs.
    public static File lastDirectory;
    FileFilter saveSampleFilter;
    FileFilter pngFilter;
    FileFilter treeFilter;
    FileFilter treemapNexusFilter;
    FileFilter dotTreeFilter;
    FileFilter timingFilter;
    Histogram[][][][][][] h;
    PValueHistogram p;
    CostModel[][][][][][] c;
    Stats[][][][][][] s;
    Heuristic[][][][][][] genetic;
    List<TreeSpecs>[][][][][][] best;
    TreeMap<Double, Integer> pValueMap;
    
    public final int INFINITY = 999999999; //if you change this value, make
    // sure to update the validation code
    // in EditCosts.java and CLI.java that
    // reports when the users cost choices
    // are too large
    public static final int VERSION_NUMBER = 4; // current Jane version
    // the tools that build the executables and distributables for Jane don't
    // use this number, so they have to be changed manually when releasing future
    // versions of Jane.

    /**
     * Creates new form Design
     */
    public Design() {
        Utils.initIcons(this);
        initComponents();
        getRootPane().setDefaultButton(go_button);
        initFileFilters();
        initChooseTree();
        initSolutionTable();
        initSliderListeners();
        statsComputed(false);
        progress_bar.setVisible(false);
        stop_button.setEnabled(false);
        progress_label.setVisible(false);
        stat_text_display.setEditable(false);
        browse_cost_values_label_solve.setVisible(false);
        browse_cost_values_panel_solve.setVisible(false);
        browse_cost_values_stats.setVisible(false);
        browse_cost_values_panel_stats.setVisible(false);
        histogram_tab.setEnabled(false);
        set_beta_TextField.setEnabled(false);
        fileNotLoaded();

        // Making parts related to infestation invisible.
        infestation_label_solve.setVisible(false);
        infestation_jComboBox_solve.setVisible(false);
        infestation_label_stats.setVisible(false);
        infestation_jComboBox_stats.setVisible(false);
        
        // if we're currently on a Mac, this code ensures that the about and quit
        // options in the Jane menu work as expected.
        String os = System.getProperty("os.name");
        if (os.contains("Mac")); {
            try {
                Application app = Application.getApplication();
                MacOSAboutHandler aboutHandler = new MacOSAboutHandler(action_panel, this);
                app.setAboutHandler(aboutHandler);
                MacOSQuitHandler quitHandler = new MacOSQuitHandler(this);
                app.setQuitHandler(quitHandler);
            } catch (Exception e) {
            }
        }
    }

    private void initChooseTree() {
        // if a last viewed directory exists, we use that to initialize the
        // file chooser. If one does not exist, we just create a file chooser
        // with the default directory.
        if (lastDirectory != null) {
            chooseTree = new JFileChooser(lastDirectory);
        } else {
            chooseTree = new JFileChooser();
        }
        chooseTree.setDialogType(JFileChooser.OPEN_DIALOG);
        chooseTree.addChoosableFileFilter(treeFilter);
        chooseTree.addChoosableFileFilter(treemapNexusFilter);
        chooseTree.addChoosableFileFilter(dotTreeFilter);
        chooseTree.setFileFilter(treeFilter);
    }

    private void initSolutionTable() {
        solution_table.setAutoCreateColumnsFromModel(false);
        solutionModel = new SolutionTableModel(new ArrayList<EventSolver>());
        sorter = new TableSorter(solutionModel, solution_table.getTableHeader());
        solution_table.setModel(sorter);
        // Left align integers to match column heading alignment.
        solution_table.setDefaultRenderer(Integer.class, new LeftAlignedTableRenderer());
        // solution_table.setAutoCreateRowSorter(true);
        solution_table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    private void initSliderListeners() {
        sample_size_slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //updateTimeLabel();
            }
        });
        stat_pop_size_slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //updateTimeLabel();
            }
        });
        solve_pop_size_slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //updateTimeLabel();
            }
        });
        stat_num_generations_slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //updateTimeLabel();
            }
        });
        solve_num_generations_slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //updateTimeLabel();
            }
        });
    }

    private void initFileFilters() {
        saveSampleFilter = new FileFilter() {

            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "Microsoft Excel-Compatible CSV file (.csv)";
            }
        };
        pngFilter = new FileFilter() {

            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".png");
            }

            @Override
            public String getDescription() {
                return "png image files, (.png)";
            }
        };
        treeFilter = new FileFilter() {

            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".nex") || name.endsWith(".tgl") || name.endsWith(".tree");
            }

            @Override
            public String getDescription() {
                return "All Compatible Files (.tree, .nex, .tgl)";
            }
        };
        treemapNexusFilter = new FileFilter() {

            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".nex") || name.endsWith(".tgl");
            }

            @Override
            public String getDescription() {
                return "TreeMap Nexus files (.nex, .tgl)";
            }
        };
        dotTreeFilter = new FileFilter() {

            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".tree");
            }

            @Override
            public String getDescription() {
                return "Tarzan tree files (.tree)";
            }
        };
        timingFilter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                String name = f.getName();
                return f.isDirectory() || name.endsWith(".tmg") || (!name.contains("."));
            }

            @Override
            public String getDescription() {
                return "Jane timing files (.tmg)";
            }
        };
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        browse_cost_values_label_solve = new javax.swing.JLabel();
        tabs = new javax.swing.JTabbedPane();
        solve_panel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        solution_table = new javax.swing.JTable();
        solutions_label = new javax.swing.JLabel();
        genetic_algorithm_parameters_label = new javax.swing.JLabel();
        genetic_solve_panel = new javax.swing.JPanel();
        solve_num_generations_slider = new edu.hmc.jane.gui.JSliderInput();
        solve_pop_size_slider = new edu.hmc.jane.gui.JSliderInput();
        number_of_generations_label_solve = new javax.swing.JLabel();
        population_size_label_solve = new javax.swing.JLabel();
        browse_cost_values_panel_solve = new javax.swing.JPanel();
        cospeciation_jComboBox_solve = new javax.swing.JComboBox();
        duplication_jComboBox_solve = new javax.swing.JComboBox();
        duplication_host_switch_jComboBox_solve = new javax.swing.JComboBox();
        failure_to_diverge_jComboBox_solve = new javax.swing.JComboBox();
        loss_jComboBox_solve = new javax.swing.JComboBox();
        infestation_jComboBox_solve = new javax.swing.JComboBox();
        cospeciation_label_solve = new javax.swing.JLabel();
        duplication_label_solve = new javax.swing.JLabel();
        duplication_host_switch_label_solve = new javax.swing.JLabel();
        loss_label_solve = new javax.swing.JLabel();
        failure_to_diverge_label_solve = new javax.swing.JLabel();
        infestation_label_solve = new javax.swing.JLabel();
        lowerBound_titleLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        lowerBound_label = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        compress_checkBox = new javax.swing.JCheckBox();
        compress_status_label = new javax.swing.JLabel();
        stats_panel = new javax.swing.JPanel();
        stat_param_panel = new javax.swing.JPanel();
        sample_size_label = new javax.swing.JLabel();
        include_original_problem_instance_checkBox = new javax.swing.JCheckBox();
        randomization_method_label = new javax.swing.JLabel();
        random_tip_mapping_radioButton = new javax.swing.JRadioButton();
        random_parasite_tree_radioButton = new javax.swing.JRadioButton();
        set_beta_TextField = new javax.swing.JTextField();
        save_sample_costs_button = new javax.swing.JButton();
        sample_size_slider = new edu.hmc.jane.gui.JSliderInput();
        jScrollPane2 = new javax.swing.JScrollPane();
        stat_text_display = new javax.swing.JTextArea();
        statistics_label = new javax.swing.JLabel();
        beta_label = new javax.swing.JLabel();
        genetic_stats_panel = new javax.swing.JPanel();
        stat_num_generations_slider = new edu.hmc.jane.gui.JSliderInput();
        stat_pop_size_slider = new edu.hmc.jane.gui.JSliderInput();
        number_of_generations_label_stats = new javax.swing.JLabel();
        population_size_label_stats = new javax.swing.JLabel();
        histogram_label = new javax.swing.JLabel();
        genetic_algorithm_parameters_label_stats = new javax.swing.JLabel();
        statistical_parameters_label = new javax.swing.JLabel();
        browse_cost_values_panel_stats = new javax.swing.JPanel();
        cospeciatoin_label_stats = new javax.swing.JLabel();
        duplication_label_stats = new javax.swing.JLabel();
        duplication_host_switch_label_stats = new javax.swing.JLabel();
        loss_label_stats = new javax.swing.JLabel();
        failure_to_diverge_label_stats = new javax.swing.JLabel();
        cospeciation_jComboBox_stats = new javax.swing.JComboBox();
        duplication_jComboBox_stats = new javax.swing.JComboBox();
        duplication_host_switch_jComboBox_stats = new javax.swing.JComboBox();
        loss_jComboBox_stats = new javax.swing.JComboBox();
        failure_to_diverge_jComboBox_stats = new javax.swing.JComboBox();
        infestation_label_stats = new javax.swing.JLabel();
        infestation_jComboBox_stats = new javax.swing.JComboBox();
        browse_cost_values_stats = new javax.swing.JLabel();
        histogram_tab = new javax.swing.JTabbedPane();
        cost_histogram_holder_panel = new javax.swing.JPanel();
        cost_histogram_panel = new javax.swing.JPanel();
        p_value_histogram_holder_panel = new javax.swing.JPanel();
        p_value_histogram_panel = new javax.swing.JPanel();
        save_as_picture_button = new javax.swing.JButton();
        show_histogram_button = new javax.swing.JButton();
        progress_bar = new javax.swing.JProgressBar();
        action_panel = new javax.swing.JPanel();
        status_label = new javax.swing.JLabel();
        go_button = new javax.swing.JButton();
        stop_button = new javax.swing.JButton();
        problem_information_panel = new javax.swing.JPanel();
        current_file_label = new javax.swing.JLabel();
        host_tips_label = new javax.swing.JLabel();
        parasite_tips_label = new javax.swing.JLabel();
        progress_label = new javax.swing.JLabel();
        problem_information_label = new javax.swing.JLabel();
        actions_label = new javax.swing.JLabel();
        menu_bar = new javax.swing.JMenuBar();
        file_menu = new javax.swing.JMenu();
        open_trees_menu_item = new javax.swing.JMenuItem();
        launch_tree_builder_menu_item = new javax.swing.JMenuItem();
        AboutDialog = new javax.swing.JMenuItem();
        quit_menu_item = new javax.swing.JMenuItem();
        settings_menu = new javax.swing.JMenu();
        set_costs_menu_option = new javax.swing.JMenuItem();
        set_HS_IN_FTD_parameters_menu_option = new javax.swing.JMenuItem();
        set_PP_menu_option = new javax.swing.JMenuItem();
        set_advanced_genetic_algorithm_parameters_menu_option = new javax.swing.JMenuItem();
        solve_mode_menu = new javax.swing.JMenu();
        add_host_timing_to_table_menu_option = new javax.swing.JMenuItem();
        adjust_number_of_solutions_menu_option = new javax.swing.JMenuItem();
        set_support_params_menuItem = new javax.swing.JMenuItem();
        clear_table_menu_option = new javax.swing.JMenuItem();

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        browse_cost_values_label_solve.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        browse_cost_values_label_solve.setText("Browse Cost Values");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Jane");
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(800, 600));

        tabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabsStateChanged(evt);
            }
        });

        solution_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "# Cospeciations", "# Duplications", "# Duplications & Host Switches", "# Losses", "# Failures To Diverge", "Cost"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        solution_table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        solution_table.setDoubleBuffered(true);
        solution_table.setMinimumSize(new java.awt.Dimension(200, 100));
        solution_table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                solution_tableMouseClicked(evt);
            }
        });
        solution_table.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                solution_tableKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                solution_tableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(solution_table);
        solution_table.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        solutions_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        solutions_label.setText("Solutions");

        genetic_algorithm_parameters_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        genetic_algorithm_parameters_label.setText("Genetic Algorithm (GA) Parameters");

        genetic_solve_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        solve_num_generations_slider.setMaximum(10000);
        solve_num_generations_slider.setMinimum(1);
        solve_num_generations_slider.setValue(100);
        solve_num_generations_slider.setVerifyInputWhenFocusTarget(false);

        solve_pop_size_slider.setMaximum(10000);
        solve_pop_size_slider.setMinimum(1);
        solve_pop_size_slider.setValue(100);

        number_of_generations_label_solve.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        number_of_generations_label_solve.setText("Number of Generations");

        population_size_label_solve.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        population_size_label_solve.setText("Population Size");

        org.jdesktop.layout.GroupLayout genetic_solve_panelLayout = new org.jdesktop.layout.GroupLayout(genetic_solve_panel);
        genetic_solve_panel.setLayout(genetic_solve_panelLayout);
        genetic_solve_panelLayout.setHorizontalGroup(
            genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(genetic_solve_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(number_of_generations_label_solve)
                    .add(population_size_label_solve))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(solve_num_generations_slider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(solve_pop_size_slider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        genetic_solve_panelLayout.setVerticalGroup(
            genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(genetic_solve_panelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(number_of_generations_label_solve)
                    .add(solve_num_generations_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(genetic_solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(solve_pop_size_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(population_size_label_solve))
                .add(72, 72, 72))
        );

        genetic_solve_panelLayout.linkSize(new java.awt.Component[] {solve_num_generations_slider, solve_pop_size_slider}, org.jdesktop.layout.GroupLayout.VERTICAL);

        browse_cost_values_panel_solve.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        cospeciation_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        cospeciation_jComboBox_solve.setEnabled(false);
        cospeciation_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cospeciation_jComboBox_solveItemStateChanged(evt);
            }
        });

        duplication_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_jComboBox_solve.setEnabled(false);
        duplication_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                duplication_jComboBox_solveItemStateChanged(evt);
            }
        });

        duplication_host_switch_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_host_switch_jComboBox_solve.setEnabled(false);
        duplication_host_switch_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                duplication_host_switch_jComboBox_solveItemStateChanged(evt);
            }
        });

        failure_to_diverge_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        failure_to_diverge_jComboBox_solve.setEnabled(false);
        failure_to_diverge_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                failure_to_diverge_jComboBox_solveItemStateChanged(evt);
            }
        });

        loss_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        loss_jComboBox_solve.setEnabled(false);
        loss_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                loss_jComboBox_solveItemStateChanged(evt);
            }
        });

        infestation_jComboBox_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        infestation_jComboBox_solve.setEnabled(false);
        infestation_jComboBox_solve.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                infestation_jComboBox_solveItemStateChanged(evt);
            }
        });

        cospeciation_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        cospeciation_label_solve.setText("Cospeciation");

        duplication_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_label_solve.setText("Duplication");

        duplication_host_switch_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_host_switch_label_solve.setText("Duplication & Host Switch");

        loss_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        loss_label_solve.setText("Loss");

        failure_to_diverge_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        failure_to_diverge_label_solve.setText("Failure to Diverge");

        infestation_label_solve.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        infestation_label_solve.setText("Infestation");

        org.jdesktop.layout.GroupLayout browse_cost_values_panel_solveLayout = new org.jdesktop.layout.GroupLayout(browse_cost_values_panel_solve);
        browse_cost_values_panel_solve.setLayout(browse_cost_values_panel_solveLayout);
        browse_cost_values_panel_solveLayout.setHorizontalGroup(
            browse_cost_values_panel_solveLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browse_cost_values_panel_solveLayout.createSequentialGroup()
                .addContainerGap()
                .add(cospeciation_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cospeciation_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_host_switch_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_host_switch_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loss_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loss_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(failure_to_diverge_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(failure_to_diverge_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(infestation_label_solve)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(infestation_jComboBox_solve, 0, 88, Short.MAX_VALUE)
                .addContainerGap())
        );
        browse_cost_values_panel_solveLayout.setVerticalGroup(
            browse_cost_values_panel_solveLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browse_cost_values_panel_solveLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(cospeciation_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(duplication_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(duplication_host_switch_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(failure_to_diverge_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(loss_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(infestation_jComboBox_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(cospeciation_label_solve)
                .add(duplication_label_solve)
                .add(duplication_host_switch_label_solve)
                .add(loss_label_solve)
                .add(failure_to_diverge_label_solve, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(infestation_label_solve))
        );

        lowerBound_titleLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lowerBound_titleLabel.setText("Theoretical Lower bound");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        lowerBound_label.setText("N/A");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(lowerBound_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 326, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(lowerBound_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        compress_checkBox.setText("Compress Isomorphic Solutions");
        compress_checkBox.setPreferredSize(new java.awt.Dimension(233, 16));
        compress_checkBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compress_checkBoxActionPerformed(evt);
            }
        });

        compress_status_label.setText("Status: Uncompressed");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(compress_checkBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(compress_status_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 186, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(65, 65, 65))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(compress_checkBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(compress_status_label))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {compress_checkBox, compress_status_label}, org.jdesktop.layout.GroupLayout.VERTICAL);

        org.jdesktop.layout.GroupLayout solve_panelLayout = new org.jdesktop.layout.GroupLayout(solve_panel);
        solve_panel.setLayout(solve_panelLayout);
        solve_panelLayout.setHorizontalGroup(
            solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(solve_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(solve_panelLayout.createSequentialGroup()
                        .add(genetic_algorithm_parameters_label)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(solve_panelLayout.createSequentialGroup()
                        .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(genetic_solve_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jScrollPane1)
                            .add(browse_cost_values_panel_solve, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, solve_panelLayout.createSequentialGroup()
                                .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(solve_panelLayout.createSequentialGroup()
                                        .add(solutions_label)
                                        .add(0, 789, Short.MAX_VALUE))
                                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(lowerBound_titleLabel)
                                    .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 230, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap())))
        );
        solve_panelLayout.setVerticalGroup(
            solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(solve_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(genetic_algorithm_parameters_label)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(genetic_solve_panel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(9, 9, 9)
                .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(solutions_label)
                    .add(lowerBound_titleLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(solve_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(32, 32, 32)
                .add(browse_cost_values_panel_solve, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
                .add(9, 9, 9))
        );

        tabs.addTab("Solve Mode", solve_panel);

        stat_param_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        sample_size_label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sample_size_label.setText("Sample Size");

        include_original_problem_instance_checkBox.setSelected(true);
        include_original_problem_instance_checkBox.setText("Include original problem instance");

        randomization_method_label.setText("Randomization Method:");

        buttonGroup1.add(random_tip_mapping_radioButton);
        random_tip_mapping_radioButton.setSelected(true);
        random_tip_mapping_radioButton.setText("Random Tip Mapping");

        buttonGroup1.add(random_parasite_tree_radioButton);
        random_parasite_tree_radioButton.setText("Random Parasite Tree, ");
        random_parasite_tree_radioButton.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                random_parasite_tree_radioButtonStateChanged(evt);
            }
        });

        set_beta_TextField.setText("-1.0");

        save_sample_costs_button.setText("Save Sample Costs");
        save_sample_costs_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_sample_costs_buttonActionPerformed(evt);
            }
        });

        sample_size_slider.setMaximum(10000);
        sample_size_slider.setMinimum(1);

        stat_text_display.setColumns(20);
        stat_text_display.setEditable(false);
        stat_text_display.setRows(5);
        jScrollPane2.setViewportView(stat_text_display);

        statistics_label.setText("Statistics");

        beta_label.setText("beta = ");

        org.jdesktop.layout.GroupLayout stat_param_panelLayout = new org.jdesktop.layout.GroupLayout(stat_param_panel);
        stat_param_panel.setLayout(stat_param_panelLayout);
        stat_param_panelLayout.setHorizontalGroup(
            stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(stat_param_panelLayout.createSequentialGroup()
                .add(6, 6, 6)
                .add(stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(stat_param_panelLayout.createSequentialGroup()
                        .add(stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(save_sample_costs_button)
                            .add(stat_param_panelLayout.createSequentialGroup()
                                .add(6, 6, 6)
                                .add(jScrollPane2)))
                        .addContainerGap())
                    .add(stat_param_panelLayout.createSequentialGroup()
                        .add(stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(include_original_problem_instance_checkBox)
                            .add(randomization_method_label)
                            .add(stat_param_panelLayout.createSequentialGroup()
                                .add(random_parasite_tree_radioButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(beta_label)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(set_beta_TextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(random_tip_mapping_radioButton)
                            .add(statistics_label)
                            .add(stat_param_panelLayout.createSequentialGroup()
                                .add(sample_size_label)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(sample_size_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 272, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(6, 6, 6))))
        );
        stat_param_panelLayout.setVerticalGroup(
            stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, stat_param_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sample_size_label)
                    .add(sample_size_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(include_original_problem_instance_checkBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(randomization_method_label)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(random_tip_mapping_radioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(stat_param_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(random_parasite_tree_radioButton)
                    .add(set_beta_TextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(beta_label))
                .add(9, 9, 9)
                .add(statistics_label)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(save_sample_costs_button)
                .addContainerGap())
        );

        stat_param_panelLayout.linkSize(new java.awt.Component[] {random_parasite_tree_radioButton, random_tip_mapping_radioButton, set_beta_TextField}, org.jdesktop.layout.GroupLayout.VERTICAL);

        genetic_stats_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        stat_num_generations_slider.setMaximum(1000);
        stat_num_generations_slider.setMinimum(1);
        stat_num_generations_slider.setValue(100);

        stat_pop_size_slider.setMaximum(50000);
        stat_pop_size_slider.setMinimum(1);
        stat_pop_size_slider.setValue(100);

        number_of_generations_label_stats.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        number_of_generations_label_stats.setText("Number of Generations");

        population_size_label_stats.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        population_size_label_stats.setText("Population Size");

        org.jdesktop.layout.GroupLayout genetic_stats_panelLayout = new org.jdesktop.layout.GroupLayout(genetic_stats_panel);
        genetic_stats_panel.setLayout(genetic_stats_panelLayout);
        genetic_stats_panelLayout.setHorizontalGroup(
            genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(genetic_stats_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(number_of_generations_label_stats)
                    .add(population_size_label_stats))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(stat_num_generations_slider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(stat_pop_size_slider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        genetic_stats_panelLayout.setVerticalGroup(
            genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(genetic_stats_panelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(number_of_generations_label_stats)
                    .add(stat_num_generations_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(genetic_stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(population_size_label_stats)
                    .add(stat_pop_size_slider, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        histogram_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        histogram_label.setText("Histogram");

        genetic_algorithm_parameters_label_stats.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        genetic_algorithm_parameters_label_stats.setText("Genetic Algorithm (GA) Parameters");

        statistical_parameters_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        statistical_parameters_label.setText("Statistical Parameters");

        browse_cost_values_panel_stats.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        cospeciatoin_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        cospeciatoin_label_stats.setText("Cospeciation");

        duplication_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_label_stats.setText("Duplication");

        duplication_host_switch_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_host_switch_label_stats.setText("Duplication & Host Switch");

        loss_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        loss_label_stats.setText("Loss");

        failure_to_diverge_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        failure_to_diverge_label_stats.setText("Failure to Diverge");

        cospeciation_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        cospeciation_jComboBox_stats.setEnabled(false);
        cospeciation_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cospeciation_jComboBox_statsItemStateChanged(evt);
            }
        });

        duplication_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_jComboBox_stats.setEnabled(false);
        duplication_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                duplication_jComboBox_statsItemStateChanged(evt);
            }
        });

        duplication_host_switch_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        duplication_host_switch_jComboBox_stats.setEnabled(false);
        duplication_host_switch_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                duplication_host_switch_jComboBox_statsItemStateChanged(evt);
            }
        });

        loss_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        loss_jComboBox_stats.setEnabled(false);
        loss_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                loss_jComboBox_statsItemStateChanged(evt);
            }
        });

        failure_to_diverge_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        failure_to_diverge_jComboBox_stats.setEnabled(false);
        failure_to_diverge_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                failure_to_diverge_jComboBox_statsItemStateChanged(evt);
            }
        });

        infestation_label_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        infestation_label_stats.setText("Infestation");

        infestation_jComboBox_stats.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        infestation_jComboBox_stats.setEnabled(false);
        infestation_jComboBox_stats.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                infestation_jComboBox_statsItemStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout browse_cost_values_panel_statsLayout = new org.jdesktop.layout.GroupLayout(browse_cost_values_panel_stats);
        browse_cost_values_panel_stats.setLayout(browse_cost_values_panel_statsLayout);
        browse_cost_values_panel_statsLayout.setHorizontalGroup(
            browse_cost_values_panel_statsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browse_cost_values_panel_statsLayout.createSequentialGroup()
                .addContainerGap()
                .add(cospeciatoin_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cospeciation_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_host_switch_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplication_host_switch_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loss_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(loss_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(failure_to_diverge_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(failure_to_diverge_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(infestation_label_stats)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(infestation_jComboBox_stats, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        browse_cost_values_panel_statsLayout.setVerticalGroup(
            browse_cost_values_panel_statsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browse_cost_values_panel_statsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(cospeciation_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(cospeciatoin_label_stats)
                .add(duplication_label_stats)
                .add(duplication_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(duplication_host_switch_label_stats)
                .add(duplication_host_switch_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(loss_label_stats)
                .add(loss_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(failure_to_diverge_label_stats)
                .add(failure_to_diverge_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(infestation_label_stats)
                .add(infestation_jComboBox_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        browse_cost_values_stats.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        browse_cost_values_stats.setText("Browse Cost Values");

        histogram_tab.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        histogram_tab.setEnabled(false);

        cost_histogram_holder_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        cost_histogram_panel.setMinimumSize(new java.awt.Dimension(390, 159));
        cost_histogram_panel.addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
            }
            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
                cost_histogram_panelAncestorResized(evt);
            }
        });

        org.jdesktop.layout.GroupLayout cost_histogram_panelLayout = new org.jdesktop.layout.GroupLayout(cost_histogram_panel);
        cost_histogram_panel.setLayout(cost_histogram_panelLayout);
        cost_histogram_panelLayout.setHorizontalGroup(
            cost_histogram_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 662, Short.MAX_VALUE)
        );
        cost_histogram_panelLayout.setVerticalGroup(
            cost_histogram_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 201, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout cost_histogram_holder_panelLayout = new org.jdesktop.layout.GroupLayout(cost_histogram_holder_panel);
        cost_histogram_holder_panel.setLayout(cost_histogram_holder_panelLayout);
        cost_histogram_holder_panelLayout.setHorizontalGroup(
            cost_histogram_holder_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(cost_histogram_holder_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(cost_histogram_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        cost_histogram_holder_panelLayout.setVerticalGroup(
            cost_histogram_holder_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(cost_histogram_holder_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(cost_histogram_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        histogram_tab.addTab("Cost Histogram", cost_histogram_holder_panel);

        p_value_histogram_holder_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        p_value_histogram_panel.addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
            }
            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
                p_value_histogram_panelAncestorResized(evt);
            }
        });

        org.jdesktop.layout.GroupLayout p_value_histogram_panelLayout = new org.jdesktop.layout.GroupLayout(p_value_histogram_panel);
        p_value_histogram_panel.setLayout(p_value_histogram_panelLayout);
        p_value_histogram_panelLayout.setHorizontalGroup(
            p_value_histogram_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 662, Short.MAX_VALUE)
        );
        p_value_histogram_panelLayout.setVerticalGroup(
            p_value_histogram_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 201, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout p_value_histogram_holder_panelLayout = new org.jdesktop.layout.GroupLayout(p_value_histogram_holder_panel);
        p_value_histogram_holder_panel.setLayout(p_value_histogram_holder_panelLayout);
        p_value_histogram_holder_panelLayout.setHorizontalGroup(
            p_value_histogram_holder_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(p_value_histogram_holder_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(p_value_histogram_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        p_value_histogram_holder_panelLayout.setVerticalGroup(
            p_value_histogram_holder_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(p_value_histogram_holder_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(p_value_histogram_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        histogram_tab.addTab("p-value Histogram", p_value_histogram_holder_panel);

        save_as_picture_button.setText("Save as Picture");
        save_as_picture_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_as_picture_buttonActionPerformed(evt);
            }
        });

        show_histogram_button.setText("Show Histogram in New Window");
        show_histogram_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                show_histogram_buttonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout stats_panelLayout = new org.jdesktop.layout.GroupLayout(stats_panel);
        stats_panel.setLayout(stats_panelLayout);
        stats_panelLayout.setHorizontalGroup(
            stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(stats_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(stats_panelLayout.createSequentialGroup()
                        .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(save_as_picture_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 131, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(browse_cost_values_stats))
                        .add(18, 18, 18)
                        .add(show_histogram_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 239, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(stats_panelLayout.createSequentialGroup()
                        .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, histogram_tab, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, genetic_stats_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(genetic_algorithm_parameters_label_stats)
                            .add(histogram_label))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(statistical_parameters_label)
                            .add(stat_param_panel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(browse_cost_values_panel_stats, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        stats_panelLayout.setVerticalGroup(
            stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(stats_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(genetic_algorithm_parameters_label_stats)
                    .add(statistical_parameters_label))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(stats_panelLayout.createSequentialGroup()
                        .add(genetic_stats_panel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(histogram_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(histogram_tab, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(stats_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(show_histogram_button)
                            .add(save_as_picture_button))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(browse_cost_values_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(stat_param_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browse_cost_values_panel_stats, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(5, 5, 5))
        );

        tabs.addTab("Stats Mode", stats_panel);

        progress_bar.setMinimum(0);

        action_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        status_label.setText("Status: Idle");

        go_button.setText("Go");
        go_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                go_buttonActionPerformed(evt);
            }
        });

        stop_button.setText("Stop");
        stop_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_buttonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout action_panelLayout = new org.jdesktop.layout.GroupLayout(action_panel);
        action_panel.setLayout(action_panelLayout);
        action_panelLayout.setHorizontalGroup(
            action_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, action_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(action_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(action_panelLayout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(stop_button))
                    .add(action_panelLayout.createSequentialGroup()
                        .add(status_label)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(go_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 133, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        action_panelLayout.linkSize(new java.awt.Component[] {go_button, stop_button}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        action_panelLayout.setVerticalGroup(
            action_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(action_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(action_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(go_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(status_label))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(stop_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        action_panelLayout.linkSize(new java.awt.Component[] {go_button, status_label, stop_button}, org.jdesktop.layout.GroupLayout.VERTICAL);

        problem_information_panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        current_file_label.setText("Current File: none");

        host_tips_label.setText("Host Tips: N/A");

        parasite_tips_label.setText("Parasite Tips: N/A");

        org.jdesktop.layout.GroupLayout problem_information_panelLayout = new org.jdesktop.layout.GroupLayout(problem_information_panel);
        problem_information_panel.setLayout(problem_information_panelLayout);
        problem_information_panelLayout.setHorizontalGroup(
            problem_information_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(problem_information_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(problem_information_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(current_file_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(problem_information_panelLayout.createSequentialGroup()
                        .add(host_tips_label)
                        .add(18, 18, 18)
                        .add(parasite_tips_label)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        problem_information_panelLayout.setVerticalGroup(
            problem_information_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(problem_information_panelLayout.createSequentialGroup()
                .addContainerGap()
                .add(current_file_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(problem_information_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(host_tips_label)
                    .add(parasite_tips_label))
                .addContainerGap())
        );

        problem_information_panelLayout.linkSize(new java.awt.Component[] {current_file_label, host_tips_label, parasite_tips_label}, org.jdesktop.layout.GroupLayout.VERTICAL);

        progress_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        progress_label.setText("Progress");

        problem_information_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        problem_information_label.setText("Problem Information");

        actions_label.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        actions_label.setText("Actions");

        file_menu.setText("File");

        open_trees_menu_item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        open_trees_menu_item.setText("Open Trees");
        open_trees_menu_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open_trees_menu_itemActionPerformed(evt);
            }
        });
        file_menu.add(open_trees_menu_item);

        launch_tree_builder_menu_item.setText("Launch Tree Editor");
        launch_tree_builder_menu_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launch_tree_builder_menu_itemActionPerformed(evt);
            }
        });
        file_menu.add(launch_tree_builder_menu_item);

        AboutDialog.setText("About Jane");
        AboutDialog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AboutDialogActionPerformed(evt);
            }
        });
        file_menu.add(AboutDialog);
        String os = System.getProperty("os.name");
        if (os.contains("Mac")) {
            file_menu.remove(AboutDialog);
        }

        quit_menu_item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quit_menu_item.setText("Quit");
        quit_menu_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quit_menu_itemActionPerformed(evt);
            }
        });
        file_menu.add(quit_menu_item);
        if (os.contains("Mac")) {
            file_menu.remove(quit_menu_item);
        }

        menu_bar.add(file_menu);

        settings_menu.setText("Settings");

        set_costs_menu_option.setText("Set Costs");
        set_costs_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_costs_menu_optionActionPerformed(evt);
            }
        });
        settings_menu.add(set_costs_menu_option);

        set_HS_IN_FTD_parameters_menu_option.setText("Set Host Switch Parameters");
        set_HS_IN_FTD_parameters_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_HS_IN_FTD_parameters_menu_optionActionPerformed(evt);
            }
        });
        settings_menu.add(set_HS_IN_FTD_parameters_menu_option);

        set_PP_menu_option.setText("Set Polytomy Parameters");
        set_PP_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_PP_menu_optionActionPerformed(evt);
            }
        });
        settings_menu.add(set_PP_menu_option);

        set_advanced_genetic_algorithm_parameters_menu_option.setText("Set Advanced GA Parameters");
        set_advanced_genetic_algorithm_parameters_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_advanced_genetic_algorithm_parameters_menu_optionActionPerformed(evt);
            }
        });
        settings_menu.add(set_advanced_genetic_algorithm_parameters_menu_option);

        menu_bar.add(settings_menu);

        solve_mode_menu.setText("Solve Mode Options");

        add_host_timing_to_table_menu_option.setText("Add Host Timing to Table");
        add_host_timing_to_table_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_host_timing_to_table_menu_optionActionPerformed(evt);
            }
        });
        solve_mode_menu.add(add_host_timing_to_table_menu_option);

        adjust_number_of_solutions_menu_option.setText("Adjust Number of Solutions");
        adjust_number_of_solutions_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjust_number_of_solutions_menu_optionActionPerformed(evt);
            }
        });
        solve_mode_menu.add(adjust_number_of_solutions_menu_option);

        set_support_params_menuItem.setText("Set Support Parameters");
        set_support_params_menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_support_params_menuItemActionPerformed(evt);
            }
        });
        solve_mode_menu.add(set_support_params_menuItem);

        clear_table_menu_option.setText("Clear Table");
        clear_table_menu_option.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_table_menu_optionActionPerformed(evt);
            }
        });
        solve_mode_menu.add(clear_table_menu_option);

        menu_bar.add(solve_mode_menu);

        setJMenuBar(menu_bar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(progress_label)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(progress_bar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(problem_information_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                            .add(layout.createSequentialGroup()
                                .add(problem_information_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(48, 48, 48)))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(actions_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(97, 97, 97))
                            .add(action_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
            .add(tabs)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(problem_information_label)
                    .add(actions_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(action_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(problem_information_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(progress_bar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(progress_label))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tabs))
        );

        layout.linkSize(new java.awt.Component[] {action_panel, problem_information_panel}, org.jdesktop.layout.GroupLayout.VERTICAL);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // GETTER METHODS: useful in gui code
    public CostTuple getCosts() {
        return costEditor.tuple;
    }

    private int getCosts(int index) {
        return costEditor.costs[index];
    }

    private int getSwitchD() {
        return distance.getHostSwitchLimit();
    }

    private int getInfestationD() {
        return distance.getInfestationLimit();
    }

    private int numGenerations() {
        if (solveMode) {
            return solve_num_generations_slider.getValue();
        }
        return stat_num_generations_slider.getValue();
    }

    private int popSize() {
        if (solveMode) {
            return solve_pop_size_slider.getValue();
        }
        return stat_pop_size_slider.getValue();
    }

    private int sampleSize() {
        return sample_size_slider.getValue();
    }

    private double getBeta() {
        return Double.parseDouble(set_beta_TextField.getText());
    }

    private double getMutationRate() {
        return geneticEditor.getM();
    }

    private double getSelectionStrength() {
        return geneticEditor.getS();
    }

    private boolean isPhiRandom() {
        return random_tip_mapping_radioButton.isSelected();
    }

    private boolean isParaRandom() {
        return random_parasite_tree_radioButton.isSelected();
    }

    private int getNumSolns() {
        return numSolutionsEditor.getValue();
    }

    private boolean includeOrig() {
        return this.include_original_problem_instance_checkBox.isSelected();
    }

    private void solution_tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_solution_tableMouseClicked
        if (evt.getClickCount() == 1) {
            int row = solution_table.getSelectedRow();

            if (solutionModel.expanded != -1 && solutionModel.compress) {
                // adjust rows after the expanded row
                if (row > solutionModel.expanded + solutionModel.compressedSolns.get(solutionModel.expanded).size()) {
                    row = row - solutionModel.compressedSolns.get(solutionModel.expanded).size();
                } else if (row > solutionModel.expanded) {
                    return;
                }
                // toggle closed
                if (solutionModel.expanded == row) {
                    solutionModel.expanded = -1;
                } else {
                    solutionModel.expanded = row;
                }
            } else {
                solutionModel.expanded = row;
            }
            solutionModel.fireTableDataChanged();
            evt.consume();
        }
        if (evt.getClickCount() == 2) {
            showSolution();
            evt.consume();
        }
}//GEN-LAST:event_solution_tableMouseClicked

    private void solution_tableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_solution_tableKeyReleased
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            showSolution();
            evt.consume();
        }
}//GEN-LAST:event_solution_tableKeyReleased
    
    private void showSolution() {
        Object sol = solution_table.getValueAt(solution_table.getSelectedRow(), 0);
        if (!(sol instanceof Integer)) { // return if main row in compressed mode
            return;
        }
        final int solNum = (Integer) sol;
        if (solNum > -1) {
            int proceed = JOptionPane.NO_OPTION;
            Runtime r = Runtime.getRuntime();
            System.gc(); //need to do this to get accurate info about memory remaining
            double totalMem = r.freeMemory() + r.maxMemory() - r.totalMemory();
            if (totalMem < memoryPerViewer) {
                proceed = JOptionPane.showConfirmDialog(this, "Opening another solution may consume more memory than is available, causing the program to crash. Continue anyway?", "Limited Memory Warning", JOptionPane.YES_NO_OPTION);
            }
            if (totalMem >= memoryPerViewer || proceed == JOptionPane.YES_OPTION) {
                JProgressDialog.runTask("Opening Solution", "Opening Solution (In progress)", this,
                    new Runnable() {
                        public void run() {
                            EventSolver selected = currentSolutions.get(solNum - 1);
                                
                            costEditor.tuple.setCospeciationCost((Integer) cospeciation_jComboBox_solve.getSelectedItem());
                            costEditor.tuple.setDuplicationCost((Integer) duplication_jComboBox_solve.getSelectedItem());
                            costEditor.tuple.setLossCost((Integer) loss_jComboBox_solve.getSelectedItem());
                            if (distance.getHostSwitchAllow()) {
                                costEditor.tuple.setHostSwitchCost((Integer) duplication_host_switch_jComboBox_solve.getSelectedItem());
                            } else {
                                costEditor.tuple.setHostSwitchCost(INFINITY);
                            }
                            if (distance.getFailureToDivergeAllow()) {
                                costEditor.tuple.setFailureToDivergeCost((Integer) failure_to_diverge_jComboBox_solve.getSelectedItem());
                            } else {
                                costEditor.tuple.setFailureToDivergeCost(INFINITY);
                            }
                            if (distance.getInfestationAllow()) {
                                costEditor.tuple.setInfestationCost((Integer) infestation_jComboBox_solve.getSelectedItem());
                            } else {
                                costEditor.tuple.setInfestationCost(INFINITY);
                            }

                            CostModel c = CostModel.getAppropriate(prob, costEditor.tuple, getSwitchD(), getInfestationD());
                            if (supportPop.c == null) {
                                supportPop.setCostModel(selected.getCostModel());
                                supportPop.setPopSize(supportParams.getPopSize());
                                supportPop.setCurrentSolutions(currentSolutions);
                            }

                            SolutionViewerInfo info = selected.getSolutionViewerInfo();

                            SolutionViewer sv = new SolutionViewer(selected, info, solNum, solutionModel, (prob.hostTree.hasPolytomy || prob.parasiteTree.hasPolytomy), supportPop);
                            sv.updateSupportValues();
                            sv.setVisible(true);
                        }
                    }
                );
            } else {
                JOptionPane.showMessageDialog(this, "Try increasing memory allocated to the Java Virtual Machine.", "Increase memory", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void add_host_timing_to_table_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_host_timing_to_table_menu_optionActionPerformed
        // creating a file chooser that uses the last-viewed dialog
        if (lastDirectory != null) {
            chooseTiming = new JFileChooser(lastDirectory);
        }
        else {
            chooseTiming = new JFileChooser();
        }
        chooseTiming.addChoosableFileFilter(timingFilter);
        int result = chooseTiming.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            // changing the last directory opened from
            lastDirectory = chooseTiming.getCurrentDirectory();
            TreeSpecs timing = null;
            TimingFileReader reader = new TimingFileReader(prob.hostTree, prob.parasiteTree, prob.timeZones);

            try {
                timing = reader.loadTimingFile(chooseTiming.getSelectedFile(), this);
            } catch (java.io.FileNotFoundException f) {
                JOptionPane.showMessageDialog(this, "We were not able to open the requested timing file : " + f.getLocalizedMessage(), "Nonexistant File", JOptionPane.ERROR_MESSAGE);
            } catch (java.io.IOException ix) {
                JOptionPane.showMessageDialog(this, "An error occurred while trying to read the file: " + ix.getLocalizedMessage(), "Error Reading File", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "The timing selected is incompatible with the current loaded tree. \nPlease load the correct tree and try again.", "Timing Incompatible", JOptionPane.ERROR_MESSAGE);
            }

            if (timing == null) {
                return;
            }

            // Add the solution to the table if appropriate
            CostModel c = CostModel.getAppropriate(prob, getCosts(), getSwitchD(), getInfestationD());

            EventSolver eSolver = new EventSolver(timing, prob.phi, prob.timeZones, c);
            int cost = eSolver.solve();
            clearTableOption("A specific timing has been solved. ");
            if (!c.isInfinity(cost)) {
                addSolutionIfUnique(eSolver);
                solutionModel.addSoln(eSolver);
                solutionModel.fireTableDataChanged();
                solution_table.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "That timing is not solvable with the current host switch distance", "Unsolvable Timing", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_add_host_timing_to_table_menu_optionActionPerformed

    private void go_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_go_buttonActionPerformed
        if (solveMode) {
            boolean choice = clearDataOption("If you want to run again, you must first clear "
                    + "the solution table. ", true, false);
            if (choice) {
                runSolve();
            }
        } else {
            boolean choice = clearDataOption("If you want to run again, you must first clear "
                    + "the statistical data. ", false, true);
            if (choice) {
                runStats();
            }
        }
    }//GEN-LAST:event_go_buttonActionPerformed

    private void tabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabsStateChanged
        boolean oldMode = solveMode;
        solveMode = tabs.getSelectedIndex() == 0;
        // Enable/Disable as needed
        if (oldMode != solveMode) {
            switchModes(solveMode);
        }
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            solve_num_generations_slider.setEnabled(false);
            solve_pop_size_slider.setEnabled(false);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(false);
        }
    }//GEN-LAST:event_tabsStateChanged

    private void save_sample_costs_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_sample_costs_buttonActionPerformed
        // creating a file chooser using the last visited directory.
        if (lastDirectory != null) {
            sampleChooser = new JFileChooser(lastDirectory);
        }
        else {
            sampleChooser = new JFileChooser();
        }
        sampleChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        sampleChooser.setMultiSelectionEnabled(false);
        // disabling the "AcceptAll" file filter, which is unneeded in the save dialog.
        sampleChooser.setAcceptAllFileFilterUsed(false);
        sampleChooser.addChoosableFileFilter(saveSampleFilter);
        int result = sampleChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String fn = sampleChooser.getSelectedFile().getName();
                String csvFilename = fn;
                String filepath = sampleChooser.getSelectedFile().getCanonicalPath();
                if (!fn.contains(".")) {
                    filepath += ".csv";
                    csvFilename += ".csv";
                } 
                // checking if the file already exists before saving.
                int nameLength = csvFilename.length();
                int fullLength = filepath.length();
                int dif = fullLength - nameLength;
                String upDirPath = filepath.substring(0, dif);
                File newFile = new File(upDirPath);
                String[] fileList = newFile.list();
                for (int f = 0; f < fileList.length; f++) {
                    if (fileList[f].equals(csvFilename)) {
                        int option = JOptionPane.showConfirmDialog(this, csvFilename + " "
                                + "already exists. Would you like to overwrite it?" , "", JOptionPane.YES_NO_OPTION);
                        // asking the user to confirm that they would like to overwrite the file.
                        if (option == JOptionPane.YES_OPTION) {
                            writeResults(currentStats, new File(filepath));
                            // changing the last directory saved to
                            lastDirectory = sampleChooser.getCurrentDirectory();
                        } else {
                            return;
                        }
                    }
                }
                // the file does not already exist, so saving without asking user.
                writeResults(currentStats, new File(filepath));
                lastDirectory =sampleChooser.getCurrentDirectory();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(rootPane, "Error writing sample to file: " + e.getLocalizedMessage(), "Error writing file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_save_sample_costs_buttonActionPerformed

    private void open_trees_menu_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open_trees_menu_itemActionPerformed
        boolean option = loadWarning();
        if (option || !fileLoaded) {
            int result = chooseTree.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                lastDirectory = chooseTree.getCurrentDirectory();
                File file = chooseTree.getSelectedFile();
                String path = file.getAbsolutePath();
                loadTrees(file, path, "");
            }
        }
    }//GEN-LAST:event_open_trees_menu_itemActionPerformed

    private void adjust_number_of_solutions_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adjust_number_of_solutions_menu_optionActionPerformed
        boolean choice = clearDataOption("In order to change the number of solutions, you must first clear "
                + "the solution table.", true, false);
        numSolutionsEditor.openWithEditStatus(choice);
    }//GEN-LAST:event_adjust_number_of_solutions_menu_optionActionPerformed

    private void set_advanced_genetic_algorithm_parameters_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_advanced_genetic_algorithm_parameters_menu_optionActionPerformed
        boolean choice = clearDataOption("In order to change the genetic parameters, you must first clear "
                + "the solution table and statistics data. ", true, true);
        geneticEditor.openWithEditStatus(choice);
    }//GEN-LAST:event_set_advanced_genetic_algorithm_parameters_menu_optionActionPerformed

    private void quit_menu_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quit_menu_itemActionPerformed
        int option = JOptionPane.showConfirmDialog(this, "Quitting will cause you to lose the current data. \nQuit?", "Quit?", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }//GEN-LAST:event_quit_menu_itemActionPerformed

    private void clear_table_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_table_menu_optionActionPerformed
        if (currentSolutions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "There are no solutions to clear!", "Table Empty", JOptionPane.ERROR_MESSAGE);
        } else {
            clearTableOption("");
        }
    }//GEN-LAST:event_clear_table_menu_optionActionPerformed

    private void set_HS_IN_FTD_parameters_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_HS_IN_FTD_parameters_menu_optionActionPerformed
        boolean choice = clearDataOption("In order to change the maximum host switch distance or infestation distance, you must first clear "
                + "the solution table and statistics data. ", true, true);
        distance.openWithEditStatus(choice);
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            solve_num_generations_slider.setEnabled(false);
            solve_pop_size_slider.setEnabled(false);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(false);
        } else {
            solve_num_generations_slider.setEnabled(true);
            solve_pop_size_slider.setEnabled(true);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(true);
        }
        //updateTimeLabel();
    }//GEN-LAST:event_set_HS_IN_FTD_parameters_menu_optionActionPerformed

    private void set_costs_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_costs_menu_optionActionPerformed
        boolean choice = clearDataOption("In order to change the costs, you must first clear "
                + "the solution table and statistics data.", true, true);
        costEditor.openWithEditStatus(fileLoaded, choice, distance.getHostSwitchAllow(), distance.getInfestationAllow(), distance.getFailureToDivergeAllow(), prob);
        if (!distance.getHostSwitchAllow()) {
            duplication_host_switch_jComboBox_stats.setEnabled(false);
            duplication_host_switch_jComboBox_solve.setEnabled(false);
        }
        if (!distance.getInfestationAllow()) {
            infestation_jComboBox_stats.setEnabled(false);
            infestation_jComboBox_solve.setEnabled(false);
        }
        if (!distance.getFailureToDivergeAllow()) {
            failure_to_diverge_jComboBox_stats.setEnabled(false);
            failure_to_diverge_jComboBox_solve.setEnabled(false);
        }
        if (getCosts(30) == 1) {
            browse_cost_values_label_solve.setVisible(true);
            browse_cost_values_panel_solve.setVisible(true);
            browse_cost_values_stats.setVisible(true);
            browse_cost_values_panel_stats.setVisible(true);
            if (includeOrig()) histogram_tab.setEnabled(true);
            else histogram_tab.setEnabled(false);
        } else {
            browse_cost_values_label_solve.setVisible(false);
            browse_cost_values_panel_solve.setVisible(false);
            browse_cost_values_stats.setVisible(false);
            browse_cost_values_panel_stats.setVisible(false);
            histogram_tab.setSelectedIndex(0);
            histogram_tab.setEnabled(false);
        }
        //updateTimeLabel();
    }//GEN-LAST:event_set_costs_menu_optionActionPerformed

    private void save_as_picture_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_as_picture_buttonActionPerformed
        Component curHistogram = histogram_tab.getSelectedComponent();
        Utils.promptAndSave(this, curHistogram);
}//GEN-LAST:event_save_as_picture_buttonActionPerformed

    private void show_histogram_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_show_histogram_buttonActionPerformed
        int curTab = histogram_tab.getSelectedIndex();
        Component curComponent = histogram_tab.getComponentAt(curTab);
        int curWidth = curComponent.getWidth();
        int curHeight = curComponent.getHeight();
        JPanel forWindow;
        if (curTab == 0) {
            forWindow = new Histogram(currentStats, curWidth, curHeight);
        } else {
            forWindow = new PValueHistogram(currentStats, pValueMap, curWidth, curHeight);
        }
        // registering the component so tool tips work for the new window.
        ToolTipManager.sharedInstance().registerComponent(forWindow);
        // object draws itself, no need to call any method for it.
        HistogramFrame hw = new HistogramFrame(forWindow);
}//GEN-LAST:event_show_histogram_buttonActionPerformed

    private void solution_tableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_solution_tableKeyPressed
        //this prevents the solution table from moving down to the next row
        //(like excel) when the enter key is pressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.consume();
        }
    }//GEN-LAST:event_solution_tableKeyPressed

    private void cost_histogram_panelAncestorResized(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_cost_histogram_panelAncestorResized
        if (cost_histogram_panel.getComponentCount() != 0) {
            Histogram new_h = new Histogram(currentStats, cost_histogram_panel.getWidth(), cost_histogram_panel.getHeight());
            ToolTipManager.sharedInstance().registerComponent(new_h);
            cost_histogram_panel.removeAll();
            cost_histogram_panel.add(new_h);
            cost_histogram_panel.repaint();
        }
    }//GEN-LAST:event_cost_histogram_panelAncestorResized

    private void cospeciation_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cospeciation_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            p_value_histogram_panel.removeAll();
            histogram_tab.setEnabled(true);
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        }
    }//GEN-LAST:event_cospeciation_jComboBox_statsItemStateChanged

    private void duplication_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_duplication_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            histogram_tab.setEnabled(true);
            p_value_histogram_panel.removeAll();
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        } 
    }//GEN-LAST:event_duplication_jComboBox_statsItemStateChanged

    private void duplication_host_switch_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_duplication_host_switch_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            histogram_tab.setEnabled(true);
            p_value_histogram_panel.removeAll();
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        } 
    }//GEN-LAST:event_duplication_host_switch_jComboBox_statsItemStateChanged

    private void loss_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_loss_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            histogram_tab.setEnabled(true);
            p_value_histogram_panel.removeAll();
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        } 
    }//GEN-LAST:event_loss_jComboBox_statsItemStateChanged

    private void failure_to_diverge_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_failure_to_diverge_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            histogram_tab.setEnabled(true);
            p_value_histogram_panel.removeAll();
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        }
    }//GEN-LAST:event_failure_to_diverge_jComboBox_statsItemStateChanged

    private void cospeciation_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cospeciation_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_cospeciation_jComboBox_solveItemStateChanged

    private void duplication_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_duplication_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_duplication_jComboBox_solveItemStateChanged

    private void duplication_host_switch_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_duplication_host_switch_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_duplication_host_switch_jComboBox_solveItemStateChanged

    private void loss_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_loss_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_loss_jComboBox_solveItemStateChanged

    private void failure_to_diverge_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_failure_to_diverge_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_failure_to_diverge_jComboBox_solveItemStateChanged

    private void stop_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_buttonActionPerformed
        stop_now = true;
        blink.stop();
        if (tabs.getSelectedIndex() == 0) {
            clearTable();
        } else {
            clearStats();
        }
    }//GEN-LAST:event_stop_buttonActionPerformed

    private void set_PP_menu_optionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_PP_menu_optionActionPerformed
        boolean choice = clearDataOption("In order to change the polytomy parameters, you must first clear "
                + "the solution table and statistics data. ", true, true);
        polytomyParams.openWithEditStatus(choice);
        if (polytomyParams.wasChanged()) {
            changedPP();
        }
    }//GEN-LAST:event_set_PP_menu_optionActionPerformed

    private void infestation_jComboBox_statsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_infestation_jComboBox_statsItemStateChanged
        cost_histogram_panel.removeAll();
        if (computed) {
            currentStats = s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()].data;
            initStatWindow(s[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
            cost_histogram_panel.add(h[cospeciation_jComboBox_stats.getSelectedIndex()][duplication_jComboBox_stats.getSelectedIndex()][duplication_host_switch_jComboBox_stats.getSelectedIndex()][loss_jComboBox_stats.getSelectedIndex()][failure_to_diverge_jComboBox_stats.getSelectedIndex()][infestation_jComboBox_stats.getSelectedIndex()]);
        }
        cost_histogram_panel.repaint();
        ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        if (p != null && includeOrig()) {
            histogram_tab.setEnabled(true);
            p_value_histogram_panel.removeAll();
            p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            p_value_histogram_panel.add(p);
            p_value_histogram_panel.repaint();
        }
    }//GEN-LAST:event_infestation_jComboBox_statsItemStateChanged

    private void infestation_jComboBox_solveItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_infestation_jComboBox_solveItemStateChanged
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (computed) {
            List<EventSolver> solvers = new Vector<EventSolver>();
            EventSolver eSolver;
            for (TreeSpecs timing : best[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]) {
                Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[cospeciation_jComboBox_solve.getSelectedIndex()][duplication_jComboBox_solve.getSelectedIndex()][duplication_host_switch_jComboBox_solve.getSelectedIndex()][loss_jComboBox_solve.getSelectedIndex()][failure_to_diverge_jComboBox_solve.getSelectedIndex()][infestation_jComboBox_solve.getSelectedIndex()]);
                solvers.add(eSolver);
                eSolver.setDoneComputing(exec.submit((Callable) eSolver));
            }
            currentSolutions = solutionModel.addSolns(solvers);
            solutionModel.fireTableDataChanged();
            if (currentSolutions == null) {
                currentSolutions = solvers;
            } else {
                for (EventSolver sln : solvers) {
                    addSolutionIfUnique(sln);
                }
            }
        }
        solution_table.repaint();
    }//GEN-LAST:event_infestation_jComboBox_solveItemStateChanged

    private void launch_tree_builder_menu_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launch_tree_builder_menu_itemActionPerformed
        runBeeTree();
    }//GEN-LAST:event_launch_tree_builder_menu_itemActionPerformed

    private void compress_checkBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compress_checkBoxActionPerformed
        
        if (!currentSolutions.isEmpty() && !solutionModel.eventSolvers.isEmpty()) {    
            solutionModel.expanded = -1;
            if (compress_checkBox.isSelected()) {
                solutionModel.compress = true;
                solution_table.getColumnModel().getColumn(0).setHeaderValue("# of Solutions (ID)"); 
            } else {
                solutionModel.compress = false;
                solution_table.getColumnModel().getColumn(0).setHeaderValue("ID");
            }
            if (solutionModel.compress && solutionModel.once_compressed == false) {
                showMessageWhileCompressing();
                solutionModel.fireTableDataChanged();
            }else if(solutionModel.compress){
                setCompressStatus(2); //compressed
            }else{
                setCompressStatus(0); //uncompressed
            }
            solutionModel.fireTableDataChanged();
        }
    }//GEN-LAST:event_compress_checkBoxActionPerformed

    private void setCompressStatus(int status){
        // 0 = uncompressed
        // 1 = in progress
        // 2 = compressed
        switch(status){
            case 0:
                compress_status_label.setText("Status: Uncompressed");
                break;
            case 1: 
                compress_status_label.setText("Status: Compressing...");
                break;
            case 2: 
                compress_status_label.setText("Status: Compressed");
                break;
            default:
                break;
        }
        compress_status_label.paintImmediately(compress_status_label.getVisibleRect());
    }
    
    private void p_value_histogram_panelAncestorResized(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_p_value_histogram_panelAncestorResized
        if (p_value_histogram_panel.getComponentCount() != 0) {
            PValueHistogram new_p = new PValueHistogram(currentStats, pValueMap, p_value_histogram_panel.getWidth(), p_value_histogram_panel.getHeight());
            ToolTipManager.sharedInstance().registerComponent(new_p);
            p_value_histogram_panel.removeAll();
            p_value_histogram_panel.add(new_p);
            p_value_histogram_panel.repaint();     
        }
    }//GEN-LAST:event_p_value_histogram_panelAncestorResized

    private void AboutDialogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutDialogActionPerformed
        String message = "Version " + VERSION_NUMBER + "\n"
                + "\n"
                + "Jane was developed with generous support from the National Science Foundation \n"
                + "and HHMI in the lab of Ran Libeskind-Hadas at Harvey Mudd College.  The Jane \n"
                + "design and development team includes Chris Conow, Daniel Fielder, Yaniv Ovadia, \n"
                + "Benjamin Cousins, John Peebles, Tselil Schramm, Anak Yodpinyanee, Kevin Black, \n"
                + "Rebecca Thomas, David Lingenbrink, Ki Wan Gkoo, Nicole Wein, Andrew Michaud, \n"
                + "Jordan Ezzell, Bea Metitiri, Jason Yu, and Lisa Gai. ";
        String os = System.getProperty("os.name");
        // adding the jane icon to a windows about dialog.
        if (os.contains("Windows")) {
            Image iconImage;
            try {
                // try to retrieve the jane icon and use it as the icon in the about dialog
                URL imageURL = this.getClass().getResource("/images/icon-medium.png");
                Toolkit tk = this.getToolkit();
                iconImage = tk.getImage(imageURL);
                ImageIcon aboutIcon = new ImageIcon(iconImage);
                JOptionPane.showMessageDialog(action_panel, message, "About Jane", JOptionPane.INFORMATION_MESSAGE, aboutIcon);
                // if something goes wrong, use the default about dialog icon instead.
            } catch (Exception e) {
                JOptionPane.showMessageDialog(action_panel, message, "About Jane", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        // Mac handles the icon by itself, so we don't need to bother.
        else {
            JOptionPane.showMessageDialog(action_panel, message, "About Jane", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_AboutDialogActionPerformed

    private void random_parasite_tree_radioButtonStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_random_parasite_tree_radioButtonStateChanged
        if (random_parasite_tree_radioButton.isSelected()) {
            set_beta_TextField.setEnabled(true);
        } else {
            set_beta_TextField.setEnabled(false);
        }
    }//GEN-LAST:event_random_parasite_tree_radioButtonStateChanged

    private void set_support_params_menuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_support_params_menuItemActionPerformed
        supportParams.openWithEditStatus(true);
    }//GEN-LAST:event_set_support_params_menuItemActionPerformed

    /*
     * runs tree editor
     */
    public void runBeeTree() {
        final BeeTree beetree = new BeeTree(this);
        beetree.setFrame();
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                beetree.pack();
                beetree.setVisible(true);
            }
        });
    }
    
    /*
     * warns user of losing data and clears data
     */
    public boolean loadWarning() {
        int option = 0;
        if (fileLoaded && (!currentSolutions.isEmpty() || currentStats != null)) {
            option = JOptionPane.showConfirmDialog(this, "Loading a new tree will cause you to lose all current data.\nContinue?", "Continue?", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                clearTable();
                clearStats();
                return true;
            }
            return false;
        }
        return true;
    }

    public void loadTrees(File file, String path, String ext) {
        TreeFileReader nr = null;
        try {
            nr = new NexusFileReader(path, false);
            if (!((NexusFileReader) nr).isNexus()) {
                nr = new NewNexusFileReader(path);
                if (!((NewNexusFileReader) nr).isNewNexus()) {
                    nr = new TarzanFileReader(path);
                } else {
                    JOptionPane.showMessageDialog(this, "This file is in CoRe-PA's nexus format. \nOnly the trees' structures, tip names, and tip mappings will be processed.", "Warning", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "The file " + path + " does not exist.", "File not found", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading input file: " + e.getLocalizedMessage(), "Error reading input file", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            prob = nr.readProblem();
            filename = file.getName();
            if (!(filename.endsWith(".tree") || filename.endsWith(".nex") || filename.endsWith(".TREE") || filename.endsWith(".NEX"))) {
                filename += ext;
            }
            accept_file();
        } catch (FileFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error in input file " + file.getName() + "\n" + e.getMessage(), "Malformed Input File", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading input file: " + e.getLocalizedMessage(), "Error reading input file", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void writeResults(Stats.HistoData data, File outputFile) throws IOException {
        FileWriter fw = new FileWriter(outputFile);
        fw.write(data.toString());
        fw.close();
    }

    private void changedPP() {
        prob.hostTree.conseqPoly = polytomyParams.ensure_sequential_resolutions_jCheckBox.isSelected();
        prob.parasiteTree.conseqPoly = polytomyParams.ensure_sequential_resolutions_jCheckBox.isSelected();
        prob.hostTree.noMidPolyEvents = polytomyParams.prevent_mid_polytomy_events_jCheckBox.isSelected();
        prob.parasiteTree.noMidPolyEvents = polytomyParams.prevent_mid_polytomy_events_jCheckBox.isSelected();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        String err = "Couldn't use system-specific look and feel.";
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            System.err.println(err);
        } catch (InstantiationException ex) {
            System.err.println(err);
        } catch (IllegalAccessException ex) {
            System.err.println(err);
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println(err);
        }

        final Design d = new Design();
        Thread.setDefaultUncaughtExceptionHandler(d);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                d.setVisible(true);
            }
        });

        if (!Jane.is64BitJVM()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    JOptionPane.showMessageDialog(d,
                            "Warning: You can make Jane about 2x faster\n"
                            + "by using a 64-Bit JVM instead of the 32-Bit\n"
                            + "one you are currently using. See the item\n"
                            + "\"How do I make Jane run faster?\" in the\n"
                            + "Jane FAQ at cs.hmc.edu/~hadas/jane.",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }

    void addSolutionIfUnique(EventSolver sln) {
        boolean acceptable = true;
        for (EventSolver existingsln : currentSolutions) {
            if (existingsln.equals(sln)) {
                acceptable = false;
            }
        }
        if (acceptable) {
            currentSolutions.add(sln);
            solutionModel.addSolns(currentSolutions);
        }
    }

    void switchModes(boolean solve) {
        if (fileLoaded) {
            solve_mode_menu.setEnabled(solve);
        }
        //updateTimeLabel();
    }

    private void updateTimeLabel() {
        //estimated_time_label.setText("Estimated Time: " + estimate());
    }

    void runStats() {
        if (!running) {
            int option = JOptionPane.YES_OPTION;
            if (currentStats != null) {
                option = JOptionPane.showConfirmDialog(this.stats_panel, "Running a new sample will cause you to lose the current data. \nContinue?", "Cancel and Save Results Before Computing", JOptionPane.YES_NO_OPTION);
            }
            if (option == JOptionPane.YES_OPTION) {
                // Clear cost_histogram_panel and p_value_histogram_panel and other boolean variables
                statsComputed(false);
                computed = false;
                cost_histogram_panel.removeAll();
                cost_histogram_panel.repaint();
                p = null;
                p_value_histogram_panel.removeAll();
                p_value_histogram_panel.repaint();
                ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
                setRunStatus(true);
                stop_now = false;

                blink = new Thread(new Runnable() {

                    public void run() {
                        // Empty all combo boxes in stats mode
                        cospeciation_jComboBox_stats.removeAllItems();
                        duplication_jComboBox_stats.removeAllItems();
                        duplication_host_switch_jComboBox_stats.removeAllItems();
                        loss_jComboBox_stats.removeAllItems();
                        failure_to_diverge_jComboBox_stats.removeAllItems();
                        infestation_jComboBox_stats.removeAllItems();
                        
                        if (getCosts(30) == 0) { // At "Set Costs" mode, put cost values in combo boxes
                            cospeciation_jComboBox_stats.addItem(getCosts(0));
                            duplication_jComboBox_stats.addItem(getCosts(1));
                            duplication_host_switch_jComboBox_stats.addItem(getCosts(2));
                            loss_jComboBox_stats.addItem(getCosts(3));
                            failure_to_diverge_jComboBox_stats.addItem(getCosts(4));
                            infestation_jComboBox_stats.addItem(getCosts(5));
                        } else { // At "Range Costs" mode, put cost values in combo boxes
                            if (getCosts(18) != 0) {
                                for (int a = 0; a <= getCosts(24); a++) {
                                    cospeciation_jComboBox_stats.addItem(getCosts(12) + a * getCosts(18));
                                }
                            } else {
                                cospeciation_jComboBox_stats.addItem(getCosts(12));
                            }
                            if (getCosts(19) != 0) {
                                for (int a = 0; a <= getCosts(25); a++) {
                                    duplication_jComboBox_stats.addItem(getCosts(13) + a * getCosts(19));
                                }
                            } else {
                                duplication_jComboBox_stats.addItem(getCosts(13));
                            }
                            if (getCosts(20) != 0) {
                                for (int a = 0; a <= getCosts(26); a++) {
                                    duplication_host_switch_jComboBox_stats.addItem(getCosts(14) + a * getCosts(20));
                                }
                            } else {
                                duplication_host_switch_jComboBox_stats.addItem(getCosts(14));
                            }
                            if (getCosts(21) != 0) {
                                for (int a = 0; a <= getCosts(27); a++) {
                                    loss_jComboBox_stats.addItem(getCosts(15) + a * getCosts(21));
                                }
                            } else {
                                loss_jComboBox_stats.addItem(getCosts(15));
                            }
                            if (getCosts(22) != 0) {
                                for (int a = 0; a <= getCosts(28); a++) {
                                    failure_to_diverge_jComboBox_stats.addItem(getCosts(16) + a * getCosts(22));
                                }
                            } else {
                                failure_to_diverge_jComboBox_stats.addItem(getCosts(16));
                            }
                            if (getCosts(23) != 0) {
                                for (int a = 0; a <= getCosts(29); a++) {
                                    infestation_jComboBox_stats.addItem(getCosts(17) + a * getCosts(23));
                                }
                            } else {
                                infestation_jComboBox_stats.addItem(getCosts(17));
                            }
                        }
                        
                        // Make CostModel and Stats
                        c = new CostModel[cospeciation_jComboBox_stats.getItemCount()][duplication_jComboBox_stats.getItemCount()][duplication_host_switch_jComboBox_stats.getItemCount()][loss_jComboBox_stats.getItemCount()][failure_to_diverge_jComboBox_stats.getItemCount()][infestation_jComboBox_stats.getItemCount()];
                        s = new Stats[cospeciation_jComboBox_stats.getItemCount()][duplication_jComboBox_stats.getItemCount()][duplication_host_switch_jComboBox_stats.getItemCount()][loss_jComboBox_stats.getItemCount()][failure_to_diverge_jComboBox_stats.getItemCount()][infestation_jComboBox_stats.getItemCount()];
                        
                        // Start statProgressBarThread
                        progress = statsProgressBarThread();
                        progress.start();

                        try {
                            if (getCosts(30) == 0) { // At "Set Costs" mode
                                // Set costs for CostModel
                                costEditor.tuple.setCospeciationCost((Integer) cospeciation_jComboBox_stats.getItemAt(0));
                                costEditor.tuple.setDuplicationCost((Integer) duplication_jComboBox_stats.getItemAt(0));
                                costEditor.tuple.setLossCost((Integer) loss_jComboBox_stats.getItemAt(0));
                                if (distance.getHostSwitchAllow()) {
                                    costEditor.tuple.setHostSwitchCost((Integer) duplication_host_switch_jComboBox_stats.getItemAt(0));
                                } else {
                                    costEditor.tuple.setHostSwitchCost(INFINITY);
                                }
                                if (distance.getFailureToDivergeAllow()) {
                                    costEditor.tuple.setFailureToDivergeCost((Integer) failure_to_diverge_jComboBox_stats.getItemAt(0));
                                } else {
                                    costEditor.tuple.setFailureToDivergeCost(INFINITY);
                                }
                                if (distance.getInfestationAllow()) {
                                    costEditor.tuple.setInfestationCost((Integer) infestation_jComboBox_stats.getItemAt(0));
                                } else {
                                    costEditor.tuple.setInfestationCost(INFINITY);
                                }
                                c[0][0][0][0][0][0] = CostModel.getAppropriate(prob, costEditor.tuple, getSwitchD(), getInfestationD());
                                
                                // Compute Stats using CostModel
                                s[0][0][0][0][0][0] = new Stats(c[0][0][0][0][0][0], prob, sampleSize(), numGenerations(),
                                                                popSize(), includeOrig(), isPhiRandom(), isParaRandom(),
                                                                getBeta(), getMutationRate(), getSelectionStrength());
                                currentStats = s[0][0][0][0][0][0].data;
                                final Stats tmpStats = s[0][0][0][0][0][0];
                                
                                SwingUtilities.invokeLater(new Runnable() {

                                    public void run() {
                                        // Render histogram
                                        h = new Histogram[1][1][1][1][1][1];
                                        h[0][0][0][0][0][0] = new Histogram(tmpStats.data, cost_histogram_panel.getWidth(), cost_histogram_panel.getHeight());
                                        cost_histogram_panel.removeAll();
                                        cost_histogram_panel.add(h[0][0][0][0][0][0]);
                                        cost_histogram_panel.repaint();
                                        histogram_tab.setSelectedIndex(0);
                                        histogram_tab.setEnabled(false);
                                        
                                        // Enable save buttons and refresh histogram panel
                                        initStatWindow(tmpStats);
                                    }
                                });
                            } else { // At "Range Costs" mode
                                // Set costs for CostModel
                                for (int aa = 0; aa < cospeciation_jComboBox_stats.getItemCount(); aa++) {
                                    for (int bb = 0; bb < duplication_jComboBox_stats.getItemCount(); bb++) {
                                        for (int cc = 0; cc < duplication_host_switch_jComboBox_stats.getItemCount(); cc++) {
                                            for (int dd = 0; dd < loss_jComboBox_stats.getItemCount(); dd++) {
                                                for (int ee = 0; ee < failure_to_diverge_jComboBox_stats.getItemCount(); ee++) {
                                                    for (int ff = 0; ff < infestation_jComboBox_stats.getItemCount(); ff++) {
                                                        costEditor.tuple.setCospeciationCost((Integer) cospeciation_jComboBox_stats.getItemAt(aa));
                                                        costEditor.tuple.setDuplicationCost((Integer) duplication_jComboBox_stats.getItemAt(bb));
                                                        costEditor.tuple.setLossCost((Integer) loss_jComboBox_stats.getItemAt(dd));
                                                        if (distance.getHostSwitchAllow()) {
                                                            costEditor.tuple.setHostSwitchCost((Integer) duplication_host_switch_jComboBox_stats.getItemAt(cc));
                                                        } else {
                                                            costEditor.tuple.setHostSwitchCost(INFINITY);
                                                        }
                                                        if (distance.getFailureToDivergeAllow()) {
                                                            costEditor.tuple.setFailureToDivergeCost((Integer) failure_to_diverge_jComboBox_stats.getItemAt(ee));
                                                        } else {
                                                            costEditor.tuple.setFailureToDivergeCost(INFINITY);
                                                        }
                                                        if (distance.getInfestationAllow()) {
                                                            costEditor.tuple.setInfestationCost((Integer) infestation_jComboBox_stats.getItemAt(ff));
                                                        } else {
                                                            costEditor.tuple.setInfestationCost(INFINITY);
                                                        }
                                                        c[aa][bb][cc][dd][ee][ff] = CostModel.getAppropriate(prob, costEditor.tuple, getSwitchD(), getInfestationD());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Compute Stats using CostModel
                                for (int aa = 0; aa < cospeciation_jComboBox_stats.getItemCount(); aa++) {
                                    for (int bb = 0; bb < duplication_jComboBox_stats.getItemCount(); bb++) {
                                        for (int cc = 0; cc < duplication_host_switch_jComboBox_stats.getItemCount(); cc++) {
                                            for (int dd = 0; dd < loss_jComboBox_stats.getItemCount(); dd++) {
                                                for (int ee = 0; ee < failure_to_diverge_jComboBox_stats.getItemCount(); ee++) {
                                                    for (int ff = 0; ff < infestation_jComboBox_stats.getItemCount(); ff++) {
                                                        s[aa][bb][cc][dd][ee][ff] = new Stats(c[aa][bb][cc][dd][ee][ff], prob, sampleSize(), numGenerations(),
                                                                                              popSize(), includeOrig(), isPhiRandom(), isParaRandom(),
                                                                                              getBeta(), getMutationRate(), getSelectionStrength());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                currentStats = s[0][0][0][0][0][0].data;
                                final Stats tmpStats = s[0][0][0][0][0][0];

                                SwingUtilities.invokeLater(new Runnable() {

                                    public void run() {
                                        // Render the histogram
                                        pValueMap = new TreeMap<Double, Integer>();
                                        h = new Histogram[cospeciation_jComboBox_stats.getItemCount()][duplication_jComboBox_stats.getItemCount()][duplication_host_switch_jComboBox_stats.getItemCount()][loss_jComboBox_stats.getItemCount()][failure_to_diverge_jComboBox_stats.getItemCount()][infestation_jComboBox_stats.getItemCount()];
                                        for (int aa = 0; aa < cospeciation_jComboBox_stats.getItemCount(); aa++) {
                                            for (int bb = 0; bb < duplication_jComboBox_stats.getItemCount(); bb++) {
                                                for (int cc = 0; cc < duplication_host_switch_jComboBox_stats.getItemCount(); cc++) {
                                                    for (int dd = 0; dd < loss_jComboBox_stats.getItemCount(); dd++) {
                                                        for (int ee = 0; ee < failure_to_diverge_jComboBox_stats.getItemCount(); ee++) {
                                                            for (int ff = 0; ff < infestation_jComboBox_stats.getItemCount(); ff++) {
                                                                h[aa][bb][cc][dd][ee][ff] = new Histogram(s[aa][bb][cc][dd][ee][ff].data,
                                                                                                          cost_histogram_panel.getWidth(),
                                                                                                          cost_histogram_panel.getHeight());
                                                                double percentile = s[aa][bb][cc][dd][ee][ff].data.percentileOfOrig;
                                                                if (!pValueMap.containsKey(percentile)) {
                                                                    pValueMap.put(percentile, 1);
                                                                } else {
                                                                    Integer newValue = (Integer) pValueMap.get(percentile) + 1;
                                                                    pValueMap.put(percentile, newValue);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        cost_histogram_panel.removeAll();
                                        cost_histogram_panel.add(h[0][0][0][0][0][0]);
                                        cost_histogram_panel.repaint();
                                        
                                        // Render p_value_histogram_panel
                                        if (includeOrig()) {
                                            p_value_histogram_panel.removeAll();
                                            histogram_tab.setEnabled(true);
                                            p = new PValueHistogram(Design.this.currentStats, pValueMap,
                                                                    p_value_histogram_panel.getWidth(),
                                                                    p_value_histogram_panel.getHeight());
                                            p_value_histogram_panel.add(p);
                                            p_value_histogram_panel.repaint();
                                        } else {
                                            p = null;
                                            p_value_histogram_panel.removeAll();
                                            histogram_tab.setSelectedIndex(0);
                                            histogram_tab.setEnabled(false);
                                        }
                                        
                                        // Enable save buttons and refresh histogram panel                                        
                                        initStatWindow(tmpStats);
                                    }
                                });
                            }
                            statsComputed(true);
                            setRunStatus(false);
                            computed = true;
                        } catch (ThreadDeath e) {
                            setRunStatus(false);
                        }
                    }
                });
                blink.start();
            }
        }
    }

    private Thread statsProgressBarThread() {
        for (int aa = 0; aa < cospeciation_jComboBox_stats.getItemCount(); aa++) {
            for (int bb = 0; bb < duplication_jComboBox_stats.getItemCount(); bb++) {
                for (int cc = 0; cc < duplication_host_switch_jComboBox_stats.getItemCount(); cc++) {
                    for (int dd = 0; dd < loss_jComboBox_stats.getItemCount(); dd++) {
                        for (int ee = 0; ee < failure_to_diverge_jComboBox_stats.getItemCount(); ee++) {
                            for (int ff = 0; ff < infestation_jComboBox_stats.getItemCount(); ff++) {
                                s[aa][bb][cc][dd][ee][ff].iterationsComplete = 0;
                            }
                        }
                    }
                }
            }
        }
        progress_bar.setValue(0);
        progress_bar.setMaximum(sampleSize() * (cospeciation_jComboBox_stats.getItemCount() * duplication_jComboBox_stats.getItemCount() * duplication_host_switch_jComboBox_stats.getItemCount() * loss_jComboBox_stats.getItemCount() * failure_to_diverge_jComboBox_stats.getItemCount() * infestation_jComboBox_stats.getItemCount()));
        statsThread = new Thread(new Runnable() {

            @SuppressWarnings("SleepWhileHoldingLock")
            public void run() {
                int possible_case;
                possible_case = sampleSize() * (cospeciation_jComboBox_stats.getItemCount() * duplication_jComboBox_stats.getItemCount() * duplication_host_switch_jComboBox_stats.getItemCount() * loss_jComboBox_stats.getItemCount() * failure_to_diverge_jComboBox_stats.getItemCount() * infestation_jComboBox_stats.getItemCount());
                while (!solveMode && Stats.iterationsComplete <= possible_case && !stop_now) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                progress_bar.setValue(Stats.iterationsComplete);
                            }
                        });
                        statsThread.sleep(10);
                    } catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                }
            }
        });
        return statsThread;
    }
    
    private static ExecutorService exec = Executors.newFixedThreadPool(Jane.getNumThreads(), DaemonThreadFactory.get());
    
    int lowerBoundCost = 0;
    void runSolve() {
        
        if (!running) {
            setRunStatus(true);
            computed = false;
            stop_now = false;
            solutionModel.once_compressed = false;
            solutionModel.compress = false;

            blink = new Thread(new Runnable() {

                public void run() {
                    // Empty all combo boxes in solve mode
                    cospeciation_jComboBox_solve.removeAllItems();
                    duplication_jComboBox_solve.removeAllItems();
                    duplication_host_switch_jComboBox_solve.removeAllItems();
                    loss_jComboBox_solve.removeAllItems();
                    failure_to_diverge_jComboBox_solve.removeAllItems();
                    infestation_jComboBox_solve.removeAllItems();
                    
                    if (getCosts(30) == 0) { // At "Set Costs" mode, put cost values in combo boxes
                        cospeciation_jComboBox_solve.addItem(getCosts(0));
                        duplication_jComboBox_solve.addItem(getCosts(1));
                        duplication_host_switch_jComboBox_solve.addItem(getCosts(2));
                        loss_jComboBox_solve.addItem(getCosts(3));
                        failure_to_diverge_jComboBox_solve.addItem(getCosts(4));
                        infestation_jComboBox_solve.addItem(getCosts(5));
                    } else { // At "Range Costs" mode, put cost values in combo boxes
                        if (getCosts(18) != 0) {
                            for (int a = 0; a <= getCosts(24); a++) {
                                cospeciation_jComboBox_solve.addItem(getCosts(12) + a * getCosts(18));
                            }
                        } else {
                            cospeciation_jComboBox_solve.addItem(getCosts(12));
                        }
                        if (getCosts(19) != 0) {
                            for (int a = 0; a <= getCosts(25); a++) {
                                duplication_jComboBox_solve.addItem(getCosts(13) + a * getCosts(19));
                            }
                        } else {
                            duplication_jComboBox_solve.addItem(getCosts(13));
                        }
                        if (getCosts(20) != 0) {
                            for (int a = 0; a <= getCosts(26); a++) {
                                duplication_host_switch_jComboBox_solve.addItem(getCosts(14) + a * getCosts(20));
                            }
                        } else {
                            duplication_host_switch_jComboBox_solve.addItem(getCosts(14));
                        }
                        if (getCosts(21) != 0) {
                            for (int a = 0; a <= getCosts(27); a++) {
                                loss_jComboBox_solve.addItem(getCosts(15) + a * getCosts(21));
                            }
                        } else {
                            loss_jComboBox_solve.addItem(getCosts(15));
                        }
                        if (getCosts(22) != 0) {
                            for (int a = 0; a <= getCosts(28); a++) {
                                failure_to_diverge_jComboBox_solve.addItem(getCosts(16) + a * getCosts(22));
                            }
                        } else {
                            failure_to_diverge_jComboBox_solve.addItem(getCosts(16));
                        }
                        if (getCosts(23) != 0) {
                            for (int a = 0; a <= getCosts(29); a++) {
                                infestation_jComboBox_solve.addItem(getCosts(17) + a * getCosts(23));
                            }
                        } else {
                            infestation_jComboBox_solve.addItem(getCosts(17));
                        }
                    }
                    
                    // Make CostModel, Heruistic, and List(Generation)
                    c = new CostModel[cospeciation_jComboBox_solve.getItemCount()][duplication_jComboBox_solve.getItemCount()][duplication_host_switch_jComboBox_solve.getItemCount()][loss_jComboBox_solve.getItemCount()][failure_to_diverge_jComboBox_solve.getItemCount()][infestation_jComboBox_solve.getItemCount()];
                    genetic = new Heuristic[cospeciation_jComboBox_solve.getItemCount()][duplication_jComboBox_solve.getItemCount()][duplication_host_switch_jComboBox_solve.getItemCount()][loss_jComboBox_solve.getItemCount()][failure_to_diverge_jComboBox_solve.getItemCount()][infestation_jComboBox_solve.getItemCount()];
                    best = new List[cospeciation_jComboBox_solve.getItemCount()][duplication_jComboBox_solve.getItemCount()][duplication_host_switch_jComboBox_solve.getItemCount()][loss_jComboBox_solve.getItemCount()][failure_to_diverge_jComboBox_solve.getItemCount()][infestation_jComboBox_solve.getItemCount()];
                    
                    // Start solveProgressBarThread
                    progress = solveProgressBarThread();
                    progress.start();

                    try {
                        if (getCosts(30) == 0) { // At "Set Costs" mode
                            // Set costs for CostModel
                            costEditor.tuple.setCospeciationCost((Integer) cospeciation_jComboBox_solve.getItemAt(0));
                            costEditor.tuple.setDuplicationCost((Integer) duplication_jComboBox_solve.getItemAt(0));
                            costEditor.tuple.setLossCost((Integer) loss_jComboBox_solve.getItemAt(0));
                            if (distance.getHostSwitchAllow()) {
                                costEditor.tuple.setHostSwitchCost((Integer) duplication_host_switch_jComboBox_solve.getItemAt(0));
                            } else {
                                costEditor.tuple.setHostSwitchCost(INFINITY);
                            }
                            if (distance.getFailureToDivergeAllow()) {
                                costEditor.tuple.setFailureToDivergeCost((Integer) failure_to_diverge_jComboBox_solve.getItemAt(0));
                            } else {
                                costEditor.tuple.setFailureToDivergeCost(INFINITY);
                            }
                            if (distance.getInfestationAllow()) {
                                costEditor.tuple.setInfestationCost((Integer) infestation_jComboBox_solve.getItemAt(0));
                            } else {
                                costEditor.tuple.setInfestationCost(INFINITY);
                            }             
                            c[0][0][0][0][0][0] = CostModel.getAppropriate(prob, costEditor.tuple, getSwitchD(), getInfestationD());
                            
                            // Calculate Heruistic using CostModel and ProblemInstance
                            genetic[0][0][0][0][0][0] = new Heuristic(prob.hostTree, prob.parasiteTree, prob.hostRegions, prob.phi, prob.timeZones, c[0][0][0][0][0][0]);
                            
                            // Caculate Generation using genetic.runEvolution
                            if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow()) {
                                if (!prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
                                    best[0][0][0][0][0][0] = genetic[0][0][0][0][0][0].runEvolution(1, 1, getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                                } else {
                                    best[0][0][0][0][0][0] = genetic[0][0][0][0][0][0].runEvolution(numGenerations(), popSize(), getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                                }
                            } else {
                                best[0][0][0][0][0][0] = genetic[0][0][0][0][0][0].runEvolution(numGenerations(), popSize(), getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                            }
                        } else { // At "Range Costs" mode
                            // Set costs for CostModel
                            for (int aa = 0; aa < cospeciation_jComboBox_solve.getItemCount(); aa++) {
                                for (int bb = 0; bb < duplication_jComboBox_solve.getItemCount(); bb++) {
                                    for (int cc = 0; cc < duplication_host_switch_jComboBox_solve.getItemCount(); cc++) {
                                        for (int dd = 0; dd < loss_jComboBox_solve.getItemCount(); dd++) {
                                            for (int ee = 0; ee < failure_to_diverge_jComboBox_solve.getItemCount(); ee++) {
                                                for (int ff = 0; ff < infestation_jComboBox_solve.getItemCount(); ff++) {
                                                    costEditor.tuple.setCospeciationCost((Integer) cospeciation_jComboBox_solve.getItemAt(aa));
                                                    costEditor.tuple.setDuplicationCost((Integer) duplication_jComboBox_solve.getItemAt(bb));
                                                    costEditor.tuple.setLossCost((Integer) loss_jComboBox_solve.getItemAt(dd));
                                                    if (distance.getHostSwitchAllow()) {
                                                        costEditor.tuple.setHostSwitchCost((Integer) duplication_host_switch_jComboBox_solve.getItemAt(cc));
                                                    } else {
                                                        costEditor.tuple.setHostSwitchCost(INFINITY);
                                                    }
                                                    if (distance.getFailureToDivergeAllow()) {
                                                        costEditor.tuple.setFailureToDivergeCost((Integer) failure_to_diverge_jComboBox_solve.getItemAt(ee));
                                                    } else {
                                                        costEditor.tuple.setFailureToDivergeCost(INFINITY);
                                                    }
                                                    if (distance.getInfestationAllow()) {
                                                        costEditor.tuple.setInfestationCost((Integer) infestation_jComboBox_solve.getItemAt(ff));
                                                    } else {
                                                        costEditor.tuple.setInfestationCost(INFINITY);
                                                    }
                                                    c[aa][bb][cc][dd][ee][ff] = CostModel.getAppropriate(prob, costEditor.tuple, getSwitchD(), getInfestationD());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Calculate Heruistic using CostModel and ProblemInstance
                            for (int aa = 0; aa < cospeciation_jComboBox_solve.getItemCount(); aa++) {
                                for (int bb = 0; bb < duplication_jComboBox_solve.getItemCount(); bb++) {
                                    for (int cc = 0; cc < duplication_host_switch_jComboBox_solve.getItemCount(); cc++) {
                                        for (int dd = 0; dd < loss_jComboBox_solve.getItemCount(); dd++) {
                                            for (int ee = 0; ee < failure_to_diverge_jComboBox_solve.getItemCount(); ee++) {
                                                for (int ff = 0; ff < infestation_jComboBox_solve.getItemCount(); ff++) {
                                                    genetic[aa][bb][cc][dd][ee][ff] = new Heuristic(prob.hostTree, prob.parasiteTree, prob.hostRegions, prob.phi, prob.timeZones, c[aa][bb][cc][dd][ee][ff]);
                                                    
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Caculate Generation using genetic.runEvolution
                            for (int aa = 0; aa < cospeciation_jComboBox_solve.getItemCount(); aa++) {
                                for (int bb = 0; bb < duplication_jComboBox_solve.getItemCount(); bb++) {
                                    for (int cc = 0; cc < duplication_host_switch_jComboBox_solve.getItemCount(); cc++) {
                                        for (int dd = 0; dd < loss_jComboBox_solve.getItemCount(); dd++) {
                                            for (int ee = 0; ee < failure_to_diverge_jComboBox_solve.getItemCount(); ee++) {
                                                for (int ff = 0; ff < infestation_jComboBox_solve.getItemCount(); ff++) {
                                                    if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow()) {
                                                        if (!prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
                                                            best[aa][bb][cc][dd][ee][ff] = genetic[aa][bb][cc][dd][ee][ff].runEvolution(1, 1, getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                                                        } else {
                                                            best[aa][bb][cc][dd][ee][ff] = genetic[aa][bb][cc][dd][ee][ff].runEvolution(numGenerations(), popSize(), getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                                                        }
                                                    } else {
                                                        best[aa][bb][cc][dd][ee][ff] = genetic[aa][bb][cc][dd][ee][ff].runEvolution(numGenerations(), popSize(), getMutationRate(), getSelectionStrength(), getNumSolns()).getSomeBestTimings();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Caculate solution using Generation
                        List<EventSolver> solvers = new Vector<EventSolver>();
                        EventSolver eSolver;
                        for (TreeSpecs timing : best[0][0][0][0][0][0]) {
                            Phi newPhi = prob.phi.newWithoutPolytomies(timing.parasiteTree.size - prob.phi.length());
                            eSolver = new EventSolver(timing, newPhi, prob.timeZones, c[0][0][0][0][0][0]);
                            solvers.add(eSolver);
                            eSolver.setDoneComputing(exec.submit((Callable) eSolver));
                        }
                        currentSolutions = solutionModel.addSolns(solvers);
                        solutionModel.fireTableDataChanged();
                        if (currentSolutions == null) {
                            currentSolutions = solvers;
                        } else {
                            for (EventSolver sln : solvers) {
                                addSolutionIfUnique(sln);
                            }
                        }
                        
                        if(!prob.hostTree.hasPolytomy || !prob.parasiteTree.hasPolytomy)
                        {
<<<<<<< HEAD
                            lowerBoundCost = LowerBound.DP(prob.hostTree, prob.parasiteTree, prob.phi, getCosts(1), getCosts(2), getCosts(3));
                            lowerBound_label.setText(Integer.toBinaryString(lowerBoundCost));
=======
                            lowerBoundCost = LowerBound.DP(prob.hostTree, prob.parasiteTree, prob.phi, getCosts());
                            lowerBound_label.setText("Lower Bound: " + lowerBoundCost);
>>>>>>> origin/master
                        }
                        else
                        {
                            lowerBound_label.setText("Lower Bound: Cannot be found for trees with polytomies");
                        }
                        
                        // Update solution_table and compute solutions if user wants
                        solutionModel.expanded = -1;
                        if (compress_checkBox.isSelected()) {
                            solutionModel.compress = true;
                            solution_table.getColumnModel().getColumn(0).setHeaderValue("# of Solutions (ID)");
                            showMessageWhileCompressing();
                            solutionModel.once_compressed = true;

                        } else {
                            solutionModel.compress = false;
                            solution_table.getColumnModel().getColumn(0).setHeaderValue("ID");
                            solutionModel.fireTableDataChanged();
                        }
                        
                        // Repaint solution_table and initialize progress_bar
                        solution_table.repaint();
                        setRunStatus(false);
                        computed = true;
                        progress_bar.setValue(progress_bar.getMinimum());
                    } catch (Heuristic.NoValidSolutionException e) {
                        javax.swing.JOptionPane.showMessageDialog(null, e.getMessage() + Heuristic.noValidSolnMessage, "Cannot solve!", javax.swing.JOptionPane.ERROR_MESSAGE);
                        setRunStatus(false);
                    } catch (TreeSpecs.InconsistentTreeException e) {
                        javax.swing.JOptionPane.showMessageDialog(null, e.getMessage() + TreeSpecs.inconsistentTreeMessage);
                        setRunStatus(false);
                    } catch (Heuristic.TreeFormatException e) {
                        javax.swing.JOptionPane.showMessageDialog(null, e.getMessage());
                        setRunStatus(false);
                    } catch (ThreadDeath e) {
                        setRunStatus(false);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            blink.start();
        }
    }

    private Thread solveProgressBarThread() {
        CostSolver.solvesDone = 0;
        progress_bar.setValue(0);

        int possible_case;
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            possible_case = 1;
        } else {
            possible_case = popSize() * numGenerations();
        }
        possible_case *= (cospeciation_jComboBox_solve.getItemCount() * duplication_jComboBox_solve.getItemCount() * duplication_host_switch_jComboBox_solve.getItemCount() * loss_jComboBox_solve.getItemCount() * failure_to_diverge_jComboBox_solve.getItemCount() * infestation_jComboBox_solve.getItemCount());

        progress_bar.setMaximum(possible_case);
        solveThread = new Thread(new Runnable() {

            public void run() {

                int possible_case;
                if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
                    possible_case = 1;
                } else {
                    possible_case = popSize() * numGenerations();
                }
                possible_case *= (cospeciation_jComboBox_solve.getItemCount() * duplication_jComboBox_solve.getItemCount() * duplication_host_switch_jComboBox_solve.getItemCount() * loss_jComboBox_solve.getItemCount() * failure_to_diverge_jComboBox_solve.getItemCount() * infestation_jComboBox_solve.getItemCount());

                while (solveMode && CostSolver.solvesDone <= possible_case && !stop_now) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                progress_bar.setValue(CostSolver.solvesDone);
                            }
                        });
                        solveThread.sleep(10);
                    } catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                }
                showMessageWhileSorting();
            }
        });
        return solveThread;
    }

    /*
     * This method shows the message that solutions are already computed, but
     * they are being sorted right now.
     */
    private void showMessageWhileSorting() {
        JProgressDialog.runTask("Sorting Solutions", "Sorting Solutions...", this, new Runnable() {

            public void run() {

                do {
                    try {
                        Thread.currentThread().sleep(10);
                    } catch (InterruptedException ie) {
                    }
                } while (!computed && !stop_now);

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                    }
                });
            }
        });
    }

    /*
     * This method shows the message that solutions are being compressed right
     * now.
     */
    private void showMessageWhileCompressing() {
        //solution_table.setIgnoreRepaint(true);
        setCompressStatus(1);
        solutionModel.compressTable();
        
        do{
        }while(!solutionModel.once_compressed);
        
        setCompressStatus(2);
        
    }

    /*
     * this method shouldn't be called from within the EDT, nor without setting
     * the run status to true
     */
    private void getSingleRunTime(int solves) {
        // number of times to solve increasing this value increases
        // the estimate'timing accuracy, but also takes longer to estimate
        try {
            CostModel c = CostModel.getAppropriate(prob, getCosts(), getSwitchD(), getInfestationD());
            Heuristic genetic = new Heuristic(prob.hostTree, prob.parasiteTree, prob.hostRegions, prob.phi, prob.timeZones, c);
            singleRunSeconds = genetic.multiSolve(solves, exec, getMutationRate(), getSelectionStrength());
        } catch (TreeSpecs.InconsistentTreeException e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage() + TreeSpecs.inconsistentTreeMessage);
        } catch (Heuristic.TreeFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private String estimate() {
        if (singleRunSeconds < 0) {
            return "N/A";
        }

        int possible_case;
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            possible_case = 1;
        } else {
            possible_case = popSize() * numGenerations();
        }
        if (getCosts(30) == 1) {
            for (int a = 0; a < 6; a++) {
                if (getCosts(a + 18) != 0) {
                    possible_case *= (getCosts(a + 24) + 1);
                }
            }
        }

        double runtime;
        if (solveMode) {
            runtime = possible_case * singleRunSeconds;
        } else {
            runtime = possible_case * sampleSize() * singleRunSeconds;
        }

        String timestr;
        if (runtime < 60) {
            timestr = String.format("%.1f sec.", runtime);
        } else if (runtime / 60 < 60) {
            timestr = String.format("%.1f min.", runtime / 60);
        } else if (runtime / 3600 < 24) {
            timestr = String.format("%.1f hrs.", runtime / 3600);
        } else {
            timestr = String.format("%.1f days", runtime / 3600 / 24);
        }

        return timestr;
    }

    private void initStatWindow(Stats s) {
        stat_text_display.setText(s.toString());
    }

    private void clearTableOption(String s) {
        if (!currentSolutions.isEmpty() && !solutionModel.eventSolvers.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(this, s + "\nWould you like to clear the solution table? \n(Clearing the solution table will cause any unsaved data to be lost)", "Clear Table?", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                clearTable();
            }
            supportPop.clearPop();
            solutionModel.compressedSupport = null;
            
        }
    }

    private void clearTable() {
        currentSolutions.clear();
        solutionModel.eventSolvers.clear();
        if (solutionModel.compressedSolns != null) {
            for (int i = 0; i < solutionModel.compressedSolns.size(); i++) {
                solutionModel.compressedSolns.get(i).clear();
            }
            solutionModel.compressedSolns.clear();
        }
        solutionModel.compressedSolns = null;
        solutionModel.expanded = -1;
        solutionModel.once_compressed = false;
        solution_table.clearSelection();
        solutionModel.fireTableDataChanged();
        solution_table.repaint();
        cospeciation_jComboBox_solve.setEnabled(false);
        duplication_jComboBox_solve.setEnabled(false);
        duplication_host_switch_jComboBox_solve.setEnabled(false);
        loss_jComboBox_solve.setEnabled(false);
        failure_to_diverge_jComboBox_solve.setEnabled(false);
        infestation_jComboBox_solve.setEnabled(false);
        computed = false;
        supportPop.clearPop();
        solutionModel.compressedSupport = null;
        lowerBound_label.setText("Lower Bound:");
        
    }

    private void clearStats() {
        currentStats = null;
        statsComputed(false);
        cost_histogram_panel.removeAll();
        cost_histogram_panel.repaint();
        p_value_histogram_panel.removeAll();
        p_value_histogram_panel.repaint();
        stat_text_display.setText("");
        cospeciation_jComboBox_stats.setEnabled(false);
        duplication_jComboBox_stats.setEnabled(false);
        duplication_host_switch_jComboBox_stats.setEnabled(false);
        loss_jComboBox_stats.setEnabled(false);
        failure_to_diverge_jComboBox_stats.setEnabled(false);
        infestation_jComboBox_stats.setEnabled(false);
        computed = false;
    }

    /*
     * returns true if the user chose to clear the table or if the table is
     * already clear and false otherwise
     */
    private boolean clearDataOption(String s, boolean clearTable, boolean clearStats) {
        if ((clearStats && currentStats != null) || (clearTable && !currentSolutions.isEmpty() && !solutionModel.eventSolvers.isEmpty())) {
            if (clearTable && clearStats) {
                s += "\nWould you like to clear the solution table and statistics results? \n(Clearing will cause any unsaved data to be lost)";
            } else if (clearStats) {
                s += "\nWould you like to clear the statistics results? \n(Clearing  will cause any unsaved data to be lost)";
            } else {
                s += "\nWould you like to clear the solution table? \n(Clearing  will cause any unsaved data to be lost)";
            }
            int option = JOptionPane.showConfirmDialog(this, s, "Clear Results?", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                if (clearTable) {
                    clearTable();
                }
                if (clearStats) {
                    clearStats();
                }
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private void accept_file() {
        fileLoaded = true;
        current_file_label.setText("Current File: " + filename);
        host_tips_label.setText("Host Tips: " + prob.hostTree.numTips);
        parasite_tips_label.setText("Parasite Tips: " + prob.parasiteTree.numTips);
        // Estimate for memory per solution viewer, tested and found to be ~accurate.
        memoryPerViewer = 40 * Math.pow(prob.parasiteTree.numTips, 3.0); //only approximate, and a bit conservative
        if (solutionModel != null) {
            clearTable();
        }

        //estimated_time_label.setText("Estimated Time: Unknown");
        singleRunSeconds = -1;
        //updateTimeLabel();
        //estimate_time_button.setEnabled(true);
        settings_menu.setEnabled(true);
        if (solveMode) {
            solve_mode_menu.setEnabled(true);
        }
        add_host_timing_to_table_menu_option.setEnabled(true);
        if (prob.hostTree.hasPolytomy || prob.parasiteTree.hasPolytomy) {
            set_PP_menu_option.setEnabled(true);
        } else {
            set_PP_menu_option.setEnabled(false);
        }
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            solve_num_generations_slider.setEnabled(false);
            solve_pop_size_slider.setEnabled(false);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(false);
        } else {
            solve_num_generations_slider.setEnabled(true);
            solve_pop_size_slider.setEnabled(true);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(true);
        }
        stat_num_generations_slider.setEnabled(true);
        stat_pop_size_slider.setEnabled(true);
        sample_size_slider.setEnabled(true);
        go_button.setEnabled(true);
    }

    private void setRunStatus(boolean val) {
        running = val;
        progress_bar.setVisible(val);
        progress_label.setVisible(val);
        progress_bar.setValue(0); // clear progress bar either way.
        if (val) {
            status_label.setText("Status: Running...");
        } else {
            status_label.setText("Status: Idle");
        }
        tabs.setEnabled(!val);
        solution_table.setEnabled(!val);
        //estimate_time_button.setEnabled(singleRunSeconds == -1 && !running);
        go_button.setEnabled(!val);
        stop_button.setEnabled(val);
        stat_pop_size_slider.setEnabled(!val);
        stat_num_generations_slider.setEnabled(!val);
        if (!distance.getHostSwitchAllow() && !distance.getInfestationAllow() && !prob.hostTree.hasPolytomy && !prob.parasiteTree.hasPolytomy) {
            solve_pop_size_slider.setEnabled(false);
            solve_num_generations_slider.setEnabled(false);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(false);
        } else {
            solve_pop_size_slider.setEnabled(!val);
            solve_num_generations_slider.setEnabled(!val);
            set_advanced_genetic_algorithm_parameters_menu_option.setEnabled(!val);
        }
        set_HS_IN_FTD_parameters_menu_option.setEnabled(!val);
        solve_mode_menu.setEnabled(!val && solveMode);
        settings_menu.setEnabled(!val);
        open_trees_menu_item.setEnabled(!val);
        include_original_problem_instance_checkBox.setEnabled(!val);
        sample_size_slider.setEnabled(!val);
        random_parasite_tree_radioButton.setEnabled(!val);
        random_tip_mapping_radioButton.setEnabled(!val);
        set_beta_TextField.setEnabled(!val);
        if (!random_parasite_tree_radioButton.isSelected()) {
            set_beta_TextField.setEnabled(false);
        }

        if (tabs.getSelectedIndex() == 1) {
            cospeciation_jComboBox_stats.setEnabled(!val);
            duplication_jComboBox_stats.setEnabled(!val);
            duplication_host_switch_jComboBox_stats.setEnabled(!val);
            loss_jComboBox_stats.setEnabled(!val);
            failure_to_diverge_jComboBox_stats.setEnabled(!val);
            infestation_jComboBox_stats.setEnabled(!val);
        } else {
            cospeciation_jComboBox_solve.setEnabled(!val);
            duplication_jComboBox_solve.setEnabled(!val);
            duplication_host_switch_jComboBox_solve.setEnabled(!val);
            loss_jComboBox_solve.setEnabled(!val);
            failure_to_diverge_jComboBox_solve.setEnabled(!val);
            infestation_jComboBox_solve.setEnabled(!val);
        }
        if (!distance.getHostSwitchAllow()) {
            duplication_host_switch_jComboBox_stats.setEnabled(false);
            duplication_host_switch_jComboBox_solve.setEnabled(false);
        }
        if (!distance.getInfestationAllow()) {
            infestation_jComboBox_stats.setEnabled(false);
            infestation_jComboBox_solve.setEnabled(false);
        }
        if (!distance.getFailureToDivergeAllow()) {
            failure_to_diverge_jComboBox_stats.setEnabled(false);
            failure_to_diverge_jComboBox_solve.setEnabled(false);
        }
    }

    private void statsComputed(boolean computed) {
        save_as_picture_button.setEnabled(computed);
        save_sample_costs_button.setEnabled(computed);
        show_histogram_button.setEnabled(computed);
        if (!computed) {
            stat_text_display.removeAll();
        } else {
            this.cost_histogram_panel.repaint();
            this.p_value_histogram_panel.repaint();
            ToolTipManager.sharedInstance().registerComponent(p_value_histogram_panel);
            ToolTipManager.sharedInstance().registerComponent(cost_histogram_panel);
        }
    }

    private void fileNotLoaded() {
        //settings_menu.setEnabled(false);
        //solve_mode_menu.setEnabled(false);
        //estimate_time_button.setEnabled(false);
        set_PP_menu_option.setEnabled(false);
        add_host_timing_to_table_menu_option.setEnabled(false);
        go_button.setEnabled(false);
    }

    public void uncaughtException(final Thread t, final Throwable e) {
        final Component parent = this;
        new Thread() {

            @Override
            public void run() {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Jane encountered an error.\n"
                        + "Please let us know about this "
                        + "by emailing ran@cs.hmc.edu.\n"
                        + "Please include a copy of the "
                        + "following error message as well "
                        + "as what you were trying to do "
                        + "when the error happened.\n\n"
                        + "Error: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }.start();
    }

    static class LeftAlignedTableRenderer extends DefaultTableCellRenderer {

        NumberFormat formatter;

        public LeftAlignedTableRenderer() {
            super();
        }

        @Override
        public void setValue(Object value) {
            // allows for both strings and integer values
            if (value instanceof Integer) {
                if (formatter == null) {
                    formatter = NumberFormat.getIntegerInstance();
                }
                setText((value == null) ? "" : formatter.format(value));
            } else {
                setText((value == null) ? "" : (String) value);
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutDialog;
    private javax.swing.JPanel action_panel;
    private javax.swing.JLabel actions_label;
    private javax.swing.JMenuItem add_host_timing_to_table_menu_option;
    private javax.swing.JMenuItem adjust_number_of_solutions_menu_option;
    private javax.swing.JLabel beta_label;
    private javax.swing.JLabel browse_cost_values_label_solve;
    private javax.swing.JPanel browse_cost_values_panel_solve;
    private javax.swing.JPanel browse_cost_values_panel_stats;
    private javax.swing.JLabel browse_cost_values_stats;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JMenuItem clear_table_menu_option;
    private javax.swing.JCheckBox compress_checkBox;
    private javax.swing.JLabel compress_status_label;
    private javax.swing.JComboBox cospeciation_jComboBox_solve;
    private javax.swing.JComboBox cospeciation_jComboBox_stats;
    private javax.swing.JLabel cospeciation_label_solve;
    private javax.swing.JLabel cospeciatoin_label_stats;
    private javax.swing.JPanel cost_histogram_holder_panel;
    private javax.swing.JPanel cost_histogram_panel;
    private javax.swing.JLabel current_file_label;
    private javax.swing.JComboBox duplication_host_switch_jComboBox_solve;
    private javax.swing.JComboBox duplication_host_switch_jComboBox_stats;
    private javax.swing.JLabel duplication_host_switch_label_solve;
    private javax.swing.JLabel duplication_host_switch_label_stats;
    private javax.swing.JComboBox duplication_jComboBox_solve;
    private javax.swing.JComboBox duplication_jComboBox_stats;
    private javax.swing.JLabel duplication_label_solve;
    private javax.swing.JLabel duplication_label_stats;
    private javax.swing.JComboBox failure_to_diverge_jComboBox_solve;
    private javax.swing.JComboBox failure_to_diverge_jComboBox_stats;
    private javax.swing.JLabel failure_to_diverge_label_solve;
    private javax.swing.JLabel failure_to_diverge_label_stats;
    private javax.swing.JMenu file_menu;
    private javax.swing.JLabel genetic_algorithm_parameters_label;
    private javax.swing.JLabel genetic_algorithm_parameters_label_stats;
    private javax.swing.JPanel genetic_solve_panel;
    private javax.swing.JPanel genetic_stats_panel;
    private javax.swing.JButton go_button;
    private javax.swing.JLabel histogram_label;
    private javax.swing.JTabbedPane histogram_tab;
    private javax.swing.JLabel host_tips_label;
    private javax.swing.JCheckBox include_original_problem_instance_checkBox;
    private javax.swing.JComboBox infestation_jComboBox_solve;
    private javax.swing.JComboBox infestation_jComboBox_stats;
    private javax.swing.JLabel infestation_label_solve;
    private javax.swing.JLabel infestation_label_stats;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuItem launch_tree_builder_menu_item;
    private javax.swing.JComboBox loss_jComboBox_solve;
    private javax.swing.JComboBox loss_jComboBox_stats;
    private javax.swing.JLabel loss_label_solve;
    private javax.swing.JLabel loss_label_stats;
    private javax.swing.JLabel lowerBound_label;
    private javax.swing.JLabel lowerBound_titleLabel;
    private javax.swing.JMenuBar menu_bar;
    private javax.swing.JLabel number_of_generations_label_solve;
    private javax.swing.JLabel number_of_generations_label_stats;
    private javax.swing.JMenuItem open_trees_menu_item;
    private javax.swing.JPanel p_value_histogram_holder_panel;
    private javax.swing.JPanel p_value_histogram_panel;
    private javax.swing.JLabel parasite_tips_label;
    private javax.swing.JLabel population_size_label_solve;
    private javax.swing.JLabel population_size_label_stats;
    private javax.swing.JLabel problem_information_label;
    private javax.swing.JPanel problem_information_panel;
    private javax.swing.JProgressBar progress_bar;
    private javax.swing.JLabel progress_label;
    private javax.swing.JMenuItem quit_menu_item;
    private javax.swing.JRadioButton random_parasite_tree_radioButton;
    private javax.swing.JRadioButton random_tip_mapping_radioButton;
    private javax.swing.JLabel randomization_method_label;
    private javax.swing.JLabel sample_size_label;
    private edu.hmc.jane.gui.JSliderInput sample_size_slider;
    private javax.swing.JButton save_as_picture_button;
    private javax.swing.JButton save_sample_costs_button;
    private javax.swing.JMenuItem set_HS_IN_FTD_parameters_menu_option;
    private javax.swing.JMenuItem set_PP_menu_option;
    private javax.swing.JMenuItem set_advanced_genetic_algorithm_parameters_menu_option;
    private javax.swing.JTextField set_beta_TextField;
    private javax.swing.JMenuItem set_costs_menu_option;
    private javax.swing.JMenuItem set_support_params_menuItem;
    private javax.swing.JMenu settings_menu;
    private javax.swing.JButton show_histogram_button;
    private javax.swing.JTable solution_table;
    private javax.swing.JLabel solutions_label;
    private javax.swing.JMenu solve_mode_menu;
    private edu.hmc.jane.gui.JSliderInput solve_num_generations_slider;
    private javax.swing.JPanel solve_panel;
    private edu.hmc.jane.gui.JSliderInput solve_pop_size_slider;
    private edu.hmc.jane.gui.JSliderInput stat_num_generations_slider;
    private javax.swing.JPanel stat_param_panel;
    private edu.hmc.jane.gui.JSliderInput stat_pop_size_slider;
    private javax.swing.JTextArea stat_text_display;
    private javax.swing.JLabel statistical_parameters_label;
    private javax.swing.JLabel statistics_label;
    private javax.swing.JPanel stats_panel;
    private javax.swing.JLabel status_label;
    private javax.swing.JButton stop_button;
    private javax.swing.JTabbedPane tabs;
    // End of variables declaration//GEN-END:variables
}
