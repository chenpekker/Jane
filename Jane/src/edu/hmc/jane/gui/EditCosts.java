package edu.hmc.jane.gui;

import edu.hmc.jane.CostTuple;
import edu.hmc.jane.ProblemInstance;
import javax.swing.JOptionPane;
import javax.swing.table.*;

/*
Copyright (c) 2009, Chris Conow, Daniel Fielder, Yaniv Ovidia, Ran Libeskind-Hadas
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the Harvey Mudd College nor the names of its
      contributors may be used to endorse or promote products derived from this
      software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * EditCosts.java
 *
 * Created on Jul 27, 2009, 10:55:27 AM
 * Modified on June 18, 2012, 4:00:00 PM
 */

/**
 *
 * @author dfielder
 * @Modified by Ki Wan Gkoo
 */
public class EditCosts extends javax.swing.JDialog {

    public static final int[] DEFAULT_COSTS = new int[] {0, 1, 2, 1, 1, 1,
                                                         0, 1, 2, 1, 1, 1,
                                                         0, 1, 2, 1, 1, 1,
                                                         1, 1, 1, 1, 1, 1,
                                                         0, 0, 0, 0, 0, 0,
                                                         0};

    public int[] costs = new int[31];
    private boolean allow_editing;

    public CostTuple tuple = new CostTuple();

    javax.swing.JFormattedTextField[] textfield;
    javax.swing.JSpinner[] spinnerfield;
    String[] names = new String[] {"Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge",
                                   "Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge",
                                   "Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge",
                                   "Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge",
                                   "Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge",
                                   "Cospeciation", "Duplication", "Host Switch & Duplication", "Loss", "Failure To Diverge"};
    
    /** Creates new form EditCosts */
    public EditCosts(javax.swing.JFrame owner) {
        Utils.initIcons(this);
        initComponents();
        getRootPane().setDefaultButton(ok_button);
        costs[0] = DEFAULT_COSTS[0];
        costs[1] = DEFAULT_COSTS[1];
        costs[2] = DEFAULT_COSTS[2];
        costs[3] = DEFAULT_COSTS[3];
        costs[4] = DEFAULT_COSTS[4];
        costs[5] = DEFAULT_COSTS[5];
        costs[6] = DEFAULT_COSTS[6];
        costs[7] = DEFAULT_COSTS[7];
        costs[8] = DEFAULT_COSTS[8];
        costs[9] = DEFAULT_COSTS[9];
        costs[10] = DEFAULT_COSTS[10];
        costs[11] = DEFAULT_COSTS[11];
        costs[12] = DEFAULT_COSTS[12];
        costs[13] = DEFAULT_COSTS[13];
        costs[14] = DEFAULT_COSTS[14];
        costs[15] = DEFAULT_COSTS[15];      
        costs[16] = DEFAULT_COSTS[16];      
        costs[17] = DEFAULT_COSTS[17];      
        costs[18] = DEFAULT_COSTS[18];      
        costs[19] = DEFAULT_COSTS[19];
        costs[20] = DEFAULT_COSTS[20];   
        costs[21] = DEFAULT_COSTS[21];
        costs[22] = DEFAULT_COSTS[22];
        costs[23] = DEFAULT_COSTS[23];
        costs[24] = DEFAULT_COSTS[24];
        costs[25] = DEFAULT_COSTS[25];
        costs[26] = DEFAULT_COSTS[26];
        costs[27] = DEFAULT_COSTS[27];
        costs[28] = DEFAULT_COSTS[28];
        costs[29] = DEFAULT_COSTS[29];
        costs[30] = DEFAULT_COSTS[30];
        
        spinnerfield = new javax.swing.JSpinner[30];
        spinnerfield[0] = cospeciation_lower_bound_jSpinner;
        spinnerfield[1] = duplication_lower_bound_jSpinner;
        spinnerfield[2] = duplication_host_switch_lower_bound_jSpinner;
        spinnerfield[3] = loss_lower_bound_jSpinner;
        spinnerfield[4] = failure_to_diverge_lower_bound_jSpinner;
        spinnerfield[5] = infestation_lower_bound_jSpinner;
        spinnerfield[6] = cospeciation_step_jSpinner;
        spinnerfield[7] = duplication_step_jSpinner;
        spinnerfield[8] = duplication_host_switch_step_jSpinner;
        spinnerfield[9] = loss_step_jSpinner;
        spinnerfield[10] = failure_to_diverge_step_jSpinner;
        spinnerfield[11] = infestation_step_jSpinner;
        spinnerfield[12] = cospeciation_number_of_steps_jSpinner;
        spinnerfield[13] = duplication_number_of_steps_jSpinner;
        spinnerfield[14] = duplication_host_switch_number_of_steps_jSpinner;
        spinnerfield[15] = loss_number_of_steps_jSpinner;
        spinnerfield[16] = failure_to_diverge_number_of_steps_jSpinner;
        spinnerfield[17] = infestation_number_of_steps_jSpinner;
        
        spinnerfield[18] = cospeciation_jSpinner_SetCosts;
        spinnerfield[19] = duplication_jSpinner_SetCosts;
        spinnerfield[20] = duplication_host_switch_jSpinner_SetCosts;
        spinnerfield[21] = loss_jSpinner_SetCosts;
        spinnerfield[22] = failure_to_diverge_jSpinner_SetCosts;
        spinnerfield[23] = infestation_jSpinner_SetCosts;
        spinnerfield[24] = cospeciation_upper_bound_jSpinner;
        spinnerfield[25] = duplication_upper_bound_jSpinner;
        spinnerfield[26] = duplication_upper_bound_jSpinner;
        spinnerfield[27] = loss_upper_bound_jSpinner;
        spinnerfield[28] = failure_to_diverge_upper_bound_jSpinner;
        spinnerfield[29] = infestation_upper_bound_jSpinner;
        
        for(int i = 0; i < 12; ++i)
            spinnerfield[i+18].setValue(costs[i]);
        for(int i = 0; i < 18; ++i)
            spinnerfield[i].setValue(costs[i+12]);
        allow_editing = true;
        
        // Making parts related to infestation invisible.
        infestation_label_setcosts.setVisible(false);
        infestation_jSpinner_SetCosts.setVisible(false);
        infestation_label_rangecosts.setVisible(false);
        infestation_lower_bound_jSpinner.setVisible(false);
        infestation_step_jSpinner.setVisible(false);
        infestation_number_of_steps_jSpinner.setVisible(false);
        infestation_upper_bound_jSpinner.setVisible(false);
        this.setSize(565, 570);
        
        // Making parts related to "step" invisble
        step_label.setVisible(false);
        cospeciation_step_jSpinner.setVisible(false);
        duplication_step_jSpinner.setVisible(false);
        duplication_host_switch_step_jSpinner.setVisible(false);
        loss_step_jSpinner.setVisible(false);
        failure_to_diverge_step_jSpinner.setVisible(false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        cancel_button = new javax.swing.JButton();
        ok_button = new javax.swing.JButton();
        default_button = new javax.swing.JButton();
        tabs = new javax.swing.JTabbedPane();
        SetCosts_jPanel = new javax.swing.JPanel();
        duplication_label_setcosts = new javax.swing.JLabel();
        duplication_host_switch_label_setcosts = new javax.swing.JLabel();
        loss_label_setcosts = new javax.swing.JLabel();
        failure_to_diverge_label_setcosts = new javax.swing.JLabel();
        infestation_label_setcosts = new javax.swing.JLabel();
        cospeciation_label_setcosts = new javax.swing.JLabel();
        cospeciation_jSpinner_SetCosts = new javax.swing.JSpinner();
        duplication_host_switch_jSpinner_SetCosts = new javax.swing.JSpinner();
        duplication_jSpinner_SetCosts = new javax.swing.JSpinner();
        failure_to_diverge_jSpinner_SetCosts = new javax.swing.JSpinner();
        loss_jSpinner_SetCosts = new javax.swing.JSpinner();
        infestation_jSpinner_SetCosts = new javax.swing.JSpinner();
        RangeCosts_jPanel = new javax.swing.JPanel();
        cospeciation_label_rangecosts = new javax.swing.JLabel();
        duplication_label_rangecosts = new javax.swing.JLabel();
        duplication_host_switch_label_rangecosts = new javax.swing.JLabel();
        loss_label_rangecosts = new javax.swing.JLabel();
        failure_to_diverge_label_rangecosts = new javax.swing.JLabel();
        cospeciation_lower_bound_jSpinner = new javax.swing.JSpinner();
        duplication_lower_bound_jSpinner = new javax.swing.JSpinner();
        duplication_host_switch_lower_bound_jSpinner = new javax.swing.JSpinner();
        loss_lower_bound_jSpinner = new javax.swing.JSpinner();
        failure_to_diverge_lower_bound_jSpinner = new javax.swing.JSpinner();
        duplication_number_of_steps_jSpinner = new javax.swing.JSpinner();
        cospeciation_number_of_steps_jSpinner = new javax.swing.JSpinner();
        duplication_host_switch_number_of_steps_jSpinner = new javax.swing.JSpinner();
        loss_number_of_steps_jSpinner = new javax.swing.JSpinner();
        failure_to_diverge_number_of_steps_jSpinner = new javax.swing.JSpinner();
        cospeciation_step_jSpinner = new javax.swing.JSpinner();
        duplication_step_jSpinner = new javax.swing.JSpinner();
        loss_step_jSpinner = new javax.swing.JSpinner();
        duplication_host_switch_step_jSpinner = new javax.swing.JSpinner();
        failure_to_diverge_step_jSpinner = new javax.swing.JSpinner();
        infestation_label_rangecosts = new javax.swing.JLabel();
        infestation_lower_bound_jSpinner = new javax.swing.JSpinner();
        infestation_step_jSpinner = new javax.swing.JSpinner();
        infestation_number_of_steps_jSpinner = new javax.swing.JSpinner();
        to_label = new javax.swing.JLabel();
        number_of_steps_label = new javax.swing.JLabel();
        step_label = new javax.swing.JLabel();
        from_label = new javax.swing.JLabel();
        cospeciation_upper_bound_jSpinner = new javax.swing.JSpinner();
        duplication_upper_bound_jSpinner = new javax.swing.JSpinner();
        duplication_host_switch_upper_bound_jSpinner = new javax.swing.JSpinner();
        loss_upper_bound_jSpinner = new javax.swing.JSpinner();
        failure_to_diverge_upper_bound_jSpinner = new javax.swing.JSpinner();
        infestation_upper_bound_jSpinner = new javax.swing.JSpinner();
        region_costs_jPanel = new javax.swing.JPanel();
        region_costs_label = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        region_costs_table = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set Costs");
        setLocationByPlatform(true);
        setModal(true);
        setResizable(false);

        cancel_button.setText("Cancel");
        cancel_button.setPreferredSize(new java.awt.Dimension(90, 30));
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        ok_button.setText("OK");
        ok_button.setPreferredSize(new java.awt.Dimension(75, 30));
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        default_button.setText("Default");
        default_button.setPreferredSize(new java.awt.Dimension(90, 30));
        default_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                default_buttonActionPerformed(evt);
            }
        });

        tabs.setMinimumSize(new java.awt.Dimension(0, 0));
        tabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabsStateChanged(evt);
            }
        });

        duplication_label_setcosts.setText("Duplication");

        duplication_host_switch_label_setcosts.setText("Duplication & Host Switch");

        loss_label_setcosts.setText("Loss");

        failure_to_diverge_label_setcosts.setText("Failure To Diverge");

        infestation_label_setcosts.setText("Infestation");

        cospeciation_label_setcosts.setText("Cospeciation");

        cospeciation_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        cospeciation_jSpinner_SetCosts.setValue(0);

        duplication_host_switch_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(2, null, null, 1));

        duplication_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));

        failure_to_diverge_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));

        loss_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));

        infestation_jSpinner_SetCosts.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));

        org.jdesktop.layout.GroupLayout SetCosts_jPanelLayout = new org.jdesktop.layout.GroupLayout(SetCosts_jPanel);
        SetCosts_jPanel.setLayout(SetCosts_jPanelLayout);
        SetCosts_jPanelLayout.setHorizontalGroup(
            SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(SetCosts_jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(infestation_label_setcosts)
                    .add(loss_label_setcosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(failure_to_diverge_label_setcosts)
                    .add(duplication_label_setcosts)
                    .add(cospeciation_label_setcosts)
                    .add(duplication_host_switch_label_setcosts))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, failure_to_diverge_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, loss_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, duplication_host_switch_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, duplication_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, cospeciation_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .add(infestation_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE))
                .addContainerGap())
        );
        SetCosts_jPanelLayout.setVerticalGroup(
            SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(SetCosts_jPanelLayout.createSequentialGroup()
                .add(40, 40, 40)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(cospeciation_label_setcosts)
                    .add(cospeciation_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(duplication_label_setcosts)
                    .add(duplication_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(duplication_host_switch_label_setcosts)
                    .add(duplication_host_switch_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(loss_label_setcosts)
                    .add(loss_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(failure_to_diverge_label_setcosts)
                    .add(failure_to_diverge_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(SetCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(infestation_jSpinner_SetCosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(infestation_label_setcosts))
                .addContainerGap())
        );

        SetCosts_jPanelLayout.linkSize(new java.awt.Component[] {duplication_host_switch_label_setcosts, duplication_jSpinner_SetCosts, duplication_label_setcosts, failure_to_diverge_label_setcosts, infestation_label_setcosts, loss_label_setcosts}, org.jdesktop.layout.GroupLayout.VERTICAL);

        SetCosts_jPanelLayout.linkSize(new java.awt.Component[] {cospeciation_jSpinner_SetCosts, cospeciation_label_setcosts}, org.jdesktop.layout.GroupLayout.VERTICAL);

        tabs.addTab("Set Costs", SetCosts_jPanel);

        cospeciation_label_rangecosts.setText("Cospeciation");

        duplication_label_rangecosts.setText("Duplication");

        duplication_host_switch_label_rangecosts.setText("Duplication & Host Switch");

        loss_label_rangecosts.setText("Loss");

        failure_to_diverge_label_rangecosts.setText("Failure To Diverge");

        cospeciation_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        cospeciation_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cospeciation_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        duplication_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        duplication_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        duplication_host_switch_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(2, -99999, 99999, 1));
        duplication_host_switch_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_host_switch_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        loss_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        loss_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loss_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        failure_to_diverge_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        failure_to_diverge_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                failure_to_diverge_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        duplication_number_of_steps_jSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        duplication_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        cospeciation_number_of_steps_jSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        cospeciation_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cospeciation_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        duplication_host_switch_number_of_steps_jSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        duplication_host_switch_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_host_switch_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        loss_number_of_steps_jSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        loss_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loss_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        failure_to_diverge_number_of_steps_jSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));
        failure_to_diverge_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                failure_to_diverge_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        cospeciation_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        cospeciation_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cospeciation_step_jSpinnerStateChanged(evt);
            }
        });

        duplication_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        duplication_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_step_jSpinnerStateChanged(evt);
            }
        });

        loss_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        loss_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loss_step_jSpinnerStateChanged(evt);
            }
        });

        duplication_host_switch_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        duplication_host_switch_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_host_switch_step_jSpinnerStateChanged(evt);
            }
        });

        failure_to_diverge_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        failure_to_diverge_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                failure_to_diverge_step_jSpinnerStateChanged(evt);
            }
        });

        infestation_label_rangecosts.setText("Infestation");

        infestation_lower_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        infestation_lower_bound_jSpinner.setMinimumSize(new java.awt.Dimension(0, 0));
        infestation_lower_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                infestation_lower_bound_jSpinnerStateChanged(evt);
            }
        });

        infestation_step_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, null, null, 1));
        infestation_step_jSpinner.setMinimumSize(new java.awt.Dimension(0, 0));
        infestation_step_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                infestation_step_jSpinnerStateChanged(evt);
            }
        });

        infestation_number_of_steps_jSpinner.setMinimumSize(new java.awt.Dimension(0, 0));
        infestation_number_of_steps_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                infestation_number_of_steps_jSpinnerStateChanged(evt);
            }
        });

        to_label.setText("Upper Bound");

        number_of_steps_label.setText("# Steps");

        step_label.setText("Step");

        from_label.setText("Lower Bound");

        cospeciation_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        cospeciation_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cospeciation_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        duplication_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        duplication_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        duplication_host_switch_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(2, -99999, 99999, 1));
        duplication_host_switch_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                duplication_host_switch_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        loss_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        loss_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                loss_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        failure_to_diverge_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        failure_to_diverge_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                failure_to_diverge_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        infestation_upper_bound_jSpinner.setModel(new javax.swing.SpinnerNumberModel(1, -99999, 99999, 1));
        infestation_upper_bound_jSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                infestation_upper_bound_jSpinnerStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout RangeCosts_jPanelLayout = new org.jdesktop.layout.GroupLayout(RangeCosts_jPanel);
        RangeCosts_jPanel.setLayout(RangeCosts_jPanelLayout);
        RangeCosts_jPanelLayout.setHorizontalGroup(
            RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(infestation_label_rangecosts)
                    .add(failure_to_diverge_label_rangecosts)
                    .add(loss_label_rangecosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_host_switch_label_rangecosts)
                    .add(duplication_label_rangecosts)
                    .add(cospeciation_label_rangecosts))
                .add(18, 18, 18)
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loss_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(failure_to_diverge_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(infestation_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_host_switch_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 91, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(cospeciation_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(from_label))
                .add(18, 18, 18)
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, failure_to_diverge_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, loss_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, duplication_host_switch_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, duplication_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, step_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(infestation_step_jSpinner, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                    .add(cospeciation_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .add(18, 18, 18)
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(cospeciation_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_host_switch_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loss_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(failure_to_diverge_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(infestation_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(to_label))
                .add(37, 37, 37)
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(cospeciation_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(number_of_steps_label)
                    .add(duplication_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(duplication_host_switch_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loss_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(failure_to_diverge_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(infestation_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        RangeCosts_jPanelLayout.linkSize(new java.awt.Component[] {cospeciation_lower_bound_jSpinner, duplication_host_switch_lower_bound_jSpinner, duplication_lower_bound_jSpinner, failure_to_diverge_lower_bound_jSpinner, infestation_lower_bound_jSpinner, loss_lower_bound_jSpinner}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        RangeCosts_jPanelLayout.linkSize(new java.awt.Component[] {cospeciation_upper_bound_jSpinner, duplication_host_switch_upper_bound_jSpinner, duplication_upper_bound_jSpinner, failure_to_diverge_upper_bound_jSpinner, infestation_upper_bound_jSpinner, loss_upper_bound_jSpinner}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        RangeCosts_jPanelLayout.linkSize(new java.awt.Component[] {cospeciation_number_of_steps_jSpinner, duplication_host_switch_number_of_steps_jSpinner, duplication_number_of_steps_jSpinner, failure_to_diverge_number_of_steps_jSpinner, infestation_number_of_steps_jSpinner, loss_number_of_steps_jSpinner}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        RangeCosts_jPanelLayout.setVerticalGroup(
            RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                .add(14, 14, 14)
                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(RangeCosts_jPanelLayout.createSequentialGroup()
                        .add(from_label)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(cospeciation_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(duplication_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(duplication_host_switch_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(loss_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(failure_to_diverge_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(RangeCosts_jPanelLayout.createSequentialGroup()
                        .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(step_label)
                            .add(to_label)
                            .add(number_of_steps_label))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(cospeciation_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(cospeciation_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(duplication_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(duplication_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(duplication_host_switch_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(duplication_host_switch_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(RangeCosts_jPanelLayout.createSequentialGroup()
                                        .add(43, 43, 43)
                                        .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(loss_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(loss_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                .add(12, 12, 12)
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(failure_to_diverge_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(failure_to_diverge_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(infestation_upper_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(infestation_number_of_steps_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                                .add(cospeciation_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(duplication_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(duplication_host_switch_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(loss_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(failure_to_diverge_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(infestation_lower_bound_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(infestation_step_jSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                    .add(RangeCosts_jPanelLayout.createSequentialGroup()
                        .add(RangeCosts_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                                .add(114, 114, 114)
                                .add(duplication_host_switch_label_rangecosts)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(loss_label_rangecosts)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(failure_to_diverge_label_rangecosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(RangeCosts_jPanelLayout.createSequentialGroup()
                                .add(28, 28, 28)
                                .add(cospeciation_label_rangecosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(duplication_label_rangecosts, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(infestation_label_rangecosts)))
                .add(43, 43, 43))
        );

        RangeCosts_jPanelLayout.linkSize(new java.awt.Component[] {cospeciation_label_rangecosts, cospeciation_lower_bound_jSpinner, cospeciation_number_of_steps_jSpinner, cospeciation_step_jSpinner, cospeciation_upper_bound_jSpinner, duplication_host_switch_label_rangecosts, duplication_host_switch_lower_bound_jSpinner, duplication_host_switch_number_of_steps_jSpinner, duplication_host_switch_step_jSpinner, duplication_host_switch_upper_bound_jSpinner, duplication_label_rangecosts, duplication_lower_bound_jSpinner, duplication_number_of_steps_jSpinner, duplication_step_jSpinner, duplication_upper_bound_jSpinner, failure_to_diverge_label_rangecosts, failure_to_diverge_lower_bound_jSpinner, failure_to_diverge_number_of_steps_jSpinner, failure_to_diverge_step_jSpinner, failure_to_diverge_upper_bound_jSpinner, infestation_label_rangecosts, infestation_lower_bound_jSpinner, infestation_number_of_steps_jSpinner, infestation_step_jSpinner, infestation_upper_bound_jSpinner, loss_label_rangecosts, loss_lower_bound_jSpinner, loss_number_of_steps_jSpinner, loss_step_jSpinner, loss_upper_bound_jSpinner}, org.jdesktop.layout.GroupLayout.VERTICAL);

        RangeCosts_jPanelLayout.linkSize(new java.awt.Component[] {from_label, number_of_steps_label, step_label, to_label}, org.jdesktop.layout.GroupLayout.VERTICAL);

        tabs.addTab("Range Costs", RangeCosts_jPanel);

        region_costs_label.setText("Region Costs");

        region_costs_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Host Node 1", "Host Node 2", "Cost"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        region_costs_table.setRowSelectionAllowed(false);
        region_costs_table.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(region_costs_table);
        if (region_costs_table.getColumnModel().getColumnCount() > 0) {
            region_costs_table.getColumnModel().getColumn(0).setResizable(false);
            region_costs_table.getColumnModel().getColumn(1).setResizable(false);
            region_costs_table.getColumnModel().getColumn(2).setResizable(false);
        }

        org.jdesktop.layout.GroupLayout region_costs_jPanelLayout = new org.jdesktop.layout.GroupLayout(region_costs_jPanel);
        region_costs_jPanel.setLayout(region_costs_jPanelLayout);
        region_costs_jPanelLayout.setHorizontalGroup(
            region_costs_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(region_costs_jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(region_costs_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(region_costs_label)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE))
                .addContainerGap())
        );
        region_costs_jPanelLayout.setVerticalGroup(
            region_costs_jPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(region_costs_jPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(region_costs_label)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(default_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 132, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(ok_button, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancel_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .add(region_costs_jPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        layout.linkSize(new java.awt.Component[] {cancel_button, default_button}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(tabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(region_costs_jPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cancel_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(ok_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(default_button, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public int[] getCosts() {
        return costs;
    }

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok_buttonActionPerformed
    {//GEN-HEADEREND:event_ok_buttonActionPerformed
        if (!allow_editing) {
            this.setVisible(false);
            return;
        }
        
        for (int i = 0; i < 6; ++i) {
            if(spinnerfield[i+18].getValue() == null || !(spinnerfield[i+18].getValue() instanceof Number)) {
                JOptionPane.showMessageDialog(this, "The value for " + names[i] + " does not appear to be a number.", "Invalid Data", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        if((Integer)spinnerfield[19].getValue() > (Integer)spinnerfield[20].getValue()){
            JOptionPane.showMessageDialog(this, "The value for Duplication cannot be greater than Duplication & Host Switch.", "Invalid Cost", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
        }
        
        for (int i = 0; i < 18; ++i) {
            if(spinnerfield[i].getValue() == null || !(spinnerfield[i].getValue() instanceof Number)) {
                JOptionPane.showMessageDialog(this, "The value for " + names[i+12] + " does not appear to be a number.", "Invalid Data", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Hold on to the older cost values in case a new one is out of range.
        int[] newCosts = new int[30];
        
        for (int i = 0; i < 12; ++i) {
            newCosts[i] = ((Number)spinnerfield[i+18].getValue()).intValue();
        }
        
        for (int i = 12; i < 30; ++i) {
            newCosts[i] = ((Number)spinnerfield[i-12].getValue()).intValue();
        }
        
        for (int i=0; i < 30; i++) {
            //fail if costs are so big that total cost might exceed infinity
            if (Math.abs(newCosts[i]) > 99999) {
                JOptionPane.showMessageDialog(this, "Event costs above 99999 or below -99999 are not allowed.");
                return;
            }
        }

        for(int i = 0; i < 30; ++i) {
            costs[i] = newCosts[i];
        }

        this.setVisible(false);
    }//GEN-LAST:event_ok_buttonActionPerformed

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel_buttonActionPerformed
    {//GEN-HEADEREND:event_cancel_buttonActionPerformed
        for(int i = 0; i < 12; ++i)
            spinnerfield[i+18].setValue(costs[i]);
        for(int i = 0; i < 18; ++i)
            spinnerfield[i].setValue(costs[i+12]);

        tabs.setSelectedIndex(0);
        this.setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void default_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_default_buttonActionPerformed
        if (allow_editing) {
            for(int i = 0; i < 12; ++i)
                spinnerfield[i+18].setValue(DEFAULT_COSTS[i]);
            for(int i = 0; i < 18; ++i)
                spinnerfield[i].setValue(DEFAULT_COSTS[i+12]);
        }
    }//GEN-LAST:event_default_buttonActionPerformed

    private void cospeciation_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cospeciation_lower_bound_jSpinnerStateChanged
        cospeciation_upper_bound_jSpinner.setValue( ((Number)cospeciation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)cospeciation_step_jSpinner.getValue()).intValue()*((Number)cospeciation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_cospeciation_lower_bound_jSpinnerStateChanged

    private void duplication_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_lower_bound_jSpinnerStateChanged
        duplication_upper_bound_jSpinner.setValue( ((Number)duplication_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_step_jSpinner.getValue()).intValue()*((Number)duplication_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_lower_bound_jSpinnerStateChanged

    private void duplication_host_switch_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_host_switch_lower_bound_jSpinnerStateChanged
        duplication_host_switch_upper_bound_jSpinner.setValue( ((Number)duplication_host_switch_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_host_switch_step_jSpinner.getValue()).intValue()*((Number)duplication_host_switch_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_host_switch_lower_bound_jSpinnerStateChanged

    private void loss_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_loss_lower_bound_jSpinnerStateChanged
        loss_upper_bound_jSpinner.setValue( ((Number)loss_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)loss_step_jSpinner.getValue()).intValue()*((Number)loss_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_loss_lower_bound_jSpinnerStateChanged

    private void failure_to_diverge_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_failure_to_diverge_lower_bound_jSpinnerStateChanged
        failure_to_diverge_upper_bound_jSpinner.setValue( ((Number)failure_to_diverge_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)failure_to_diverge_step_jSpinner.getValue()).intValue()*((Number)failure_to_diverge_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_failure_to_diverge_lower_bound_jSpinnerStateChanged

    private void duplication_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_number_of_steps_jSpinnerStateChanged
        duplication_upper_bound_jSpinner.setValue( ((Number)duplication_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_step_jSpinner.getValue()).intValue()*((Number)duplication_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_number_of_steps_jSpinnerStateChanged

    private void cospeciation_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cospeciation_number_of_steps_jSpinnerStateChanged
        cospeciation_upper_bound_jSpinner.setValue( ((Number)cospeciation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)cospeciation_step_jSpinner.getValue()).intValue()*((Number)cospeciation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_cospeciation_number_of_steps_jSpinnerStateChanged

    private void duplication_host_switch_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_host_switch_number_of_steps_jSpinnerStateChanged
        duplication_host_switch_upper_bound_jSpinner.setValue( ((Number)duplication_host_switch_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_host_switch_step_jSpinner.getValue()).intValue()*((Number)duplication_host_switch_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_host_switch_number_of_steps_jSpinnerStateChanged

    private void loss_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_loss_number_of_steps_jSpinnerStateChanged
        loss_upper_bound_jSpinner.setValue( ((Number)loss_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)loss_step_jSpinner.getValue()).intValue()*((Number)loss_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_loss_number_of_steps_jSpinnerStateChanged

    private void failure_to_diverge_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_failure_to_diverge_number_of_steps_jSpinnerStateChanged
        failure_to_diverge_upper_bound_jSpinner.setValue( ((Number)failure_to_diverge_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)failure_to_diverge_step_jSpinner.getValue()).intValue()*((Number)failure_to_diverge_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_failure_to_diverge_number_of_steps_jSpinnerStateChanged

    private void cospeciation_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cospeciation_step_jSpinnerStateChanged
        cospeciation_upper_bound_jSpinner.setValue( ((Number)cospeciation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)cospeciation_step_jSpinner.getValue()).intValue()*((Number)cospeciation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_cospeciation_step_jSpinnerStateChanged

    private void duplication_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_step_jSpinnerStateChanged
        duplication_upper_bound_jSpinner.setValue( ((Number)duplication_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_step_jSpinner.getValue()).intValue()*((Number)duplication_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_step_jSpinnerStateChanged

    private void loss_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_loss_step_jSpinnerStateChanged
        loss_upper_bound_jSpinner.setValue( ((Number)loss_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)loss_step_jSpinner.getValue()).intValue()*((Number)loss_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_loss_step_jSpinnerStateChanged

    private void duplication_host_switch_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_host_switch_step_jSpinnerStateChanged
        duplication_upper_bound_jSpinner.setValue( ((Number)duplication_host_switch_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)duplication_host_switch_step_jSpinner.getValue()).intValue()*((Number)duplication_host_switch_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_duplication_host_switch_step_jSpinnerStateChanged

    private void failure_to_diverge_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_failure_to_diverge_step_jSpinnerStateChanged
        failure_to_diverge_upper_bound_jSpinner.setValue( ((Number)failure_to_diverge_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)failure_to_diverge_step_jSpinner.getValue()).intValue()*((Number)failure_to_diverge_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_failure_to_diverge_step_jSpinnerStateChanged

    private void tabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabsStateChanged
        if (tabs.getSelectedIndex() == 0)
            costs[30] = 0;
        else
            costs[30] = 1;
    }//GEN-LAST:event_tabsStateChanged

    private void infestation_lower_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_infestation_lower_bound_jSpinnerStateChanged
        infestation_upper_bound_jSpinner.setValue( ((Number)infestation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)infestation_step_jSpinner.getValue()).intValue()*((Number)infestation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_infestation_lower_bound_jSpinnerStateChanged

    private void infestation_step_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_infestation_step_jSpinnerStateChanged
        infestation_upper_bound_jSpinner.setValue( ((Number)infestation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)infestation_step_jSpinner.getValue()).intValue()*((Number)infestation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_infestation_step_jSpinnerStateChanged

    private void infestation_number_of_steps_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_infestation_number_of_steps_jSpinnerStateChanged
        infestation_upper_bound_jSpinner.setValue( ((Number)infestation_lower_bound_jSpinner.getValue()).intValue()
                + ((Number)infestation_step_jSpinner.getValue()).intValue()*((Number)infestation_number_of_steps_jSpinner.getValue()).intValue() );
    }//GEN-LAST:event_infestation_number_of_steps_jSpinnerStateChanged

    private void cospeciation_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cospeciation_upper_bound_jSpinnerStateChanged
        cospeciation_number_of_steps_jSpinner.setValue( ((Number)cospeciation_upper_bound_jSpinner.getValue()).intValue() - ((Number)cospeciation_lower_bound_jSpinner.getValue()).intValue() );
        cospeciation_step_jSpinner.setValue(1);
    }//GEN-LAST:event_cospeciation_upper_bound_jSpinnerStateChanged

    private void duplication_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_upper_bound_jSpinnerStateChanged
        duplication_number_of_steps_jSpinner.setValue( ((Number)duplication_upper_bound_jSpinner.getValue()).intValue() - ((Number)duplication_lower_bound_jSpinner.getValue()).intValue() );
        duplication_step_jSpinner.setValue(1);
    }//GEN-LAST:event_duplication_upper_bound_jSpinnerStateChanged

    private void duplication_host_switch_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_duplication_host_switch_upper_bound_jSpinnerStateChanged
        duplication_host_switch_number_of_steps_jSpinner.setValue( ((Number)duplication_host_switch_upper_bound_jSpinner.getValue()).intValue() - ((Number)duplication_host_switch_lower_bound_jSpinner.getValue()).intValue() );
        duplication_host_switch_step_jSpinner.setValue(1);
    }//GEN-LAST:event_duplication_host_switch_upper_bound_jSpinnerStateChanged

    private void loss_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_loss_upper_bound_jSpinnerStateChanged
        loss_number_of_steps_jSpinner.setValue( ((Number)loss_upper_bound_jSpinner.getValue()).intValue() - ((Number)loss_lower_bound_jSpinner.getValue()).intValue() );
        loss_step_jSpinner.setValue(1);
    }//GEN-LAST:event_loss_upper_bound_jSpinnerStateChanged

    private void failure_to_diverge_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_failure_to_diverge_upper_bound_jSpinnerStateChanged
        failure_to_diverge_number_of_steps_jSpinner.setValue( ((Number)failure_to_diverge_upper_bound_jSpinner.getValue()).intValue() - ((Number)failure_to_diverge_lower_bound_jSpinner.getValue()).intValue() );
        failure_to_diverge_step_jSpinner.setValue(1);
    }//GEN-LAST:event_failure_to_diverge_upper_bound_jSpinnerStateChanged

    private void infestation_upper_bound_jSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_infestation_upper_bound_jSpinnerStateChanged
        infestation_number_of_steps_jSpinner.setValue( ((Number)infestation_upper_bound_jSpinner.getValue()).intValue() - ((Number)infestation_lower_bound_jSpinner.getValue()).intValue() );
        infestation_step_jSpinner.setValue(1);
    }//GEN-LAST:event_infestation_upper_bound_jSpinnerStateChanged

    public void openWithEditStatus(boolean fileLoaded, boolean val, boolean hostSwitch, boolean infestation, boolean failureToDiverge, ProblemInstance prob) {
        allow_editing = val;
        for (int i = 0; i < 30; ++i)
            spinnerfield[i].setEnabled(val);
        if (!hostSwitch) {
            spinnerfield[20].setEnabled(false);
            spinnerfield[26].setEnabled(false);
            spinnerfield[2].setEnabled(false);
            spinnerfield[8].setEnabled(false);
            spinnerfield[14].setEnabled(false);
        }
        if (!failureToDiverge) {
            spinnerfield[22].setEnabled(false);
            spinnerfield[28].setEnabled(false);
            spinnerfield[4].setEnabled(false);
            spinnerfield[10].setEnabled(false);
            spinnerfield[16].setEnabled(false);
        }
        if (!infestation) {
            spinnerfield[23].setEnabled(false);
            spinnerfield[29].setEnabled(false);
            spinnerfield[5].setEnabled(false);
            spinnerfield[11].setEnabled(false);
            spinnerfield[17].setEnabled(false);
        }
        default_button.setEnabled(val);
        cancel_button.setEnabled(val);
        tabs.setEnabled(val);
        
        String data[][] = {};
        String col[] = {"Host Node 1", "Host Node 2", "Cost"};
        DefaultTableModel model = new DefaultTableModel(data, col);
        if (fileLoaded && prob.hasRegions()) {
            while(model.getRowCount() > 0) {
                model.removeRow(model.getRowCount());
            }
            int z = 0;
            for (int i = 0; i < prob.hostRegions.numRegions+1; i++) {
                for (int j = 0; j < prob.hostRegions.numRegions+1; j++) {
                    if (prob.hostRegions.getSwitchCost()[i][j] != 0) {
                    model.insertRow(z, new Object[]{i, j, prob.hostRegions.getSwitchCost()[i][j]});
                    z++;
                    }
                }
            }
            region_costs_table.setModel(model);
            region_costs_table.repaint();
        } else {
            while(model.getRowCount() > 0) {
                model.removeRow(model.getRowCount());
            }
            region_costs_table.setModel(model);
            region_costs_table.repaint();
        }
        region_costs_jPanel.setEnabled(val);
        region_costs_table.setEnabled(val);
        this.setVisible(true);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel RangeCosts_jPanel;
    private javax.swing.JPanel SetCosts_jPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancel_button;
    private javax.swing.JSpinner cospeciation_jSpinner_SetCosts;
    private javax.swing.JLabel cospeciation_label_rangecosts;
    private javax.swing.JLabel cospeciation_label_setcosts;
    private javax.swing.JSpinner cospeciation_lower_bound_jSpinner;
    private javax.swing.JSpinner cospeciation_number_of_steps_jSpinner;
    private javax.swing.JSpinner cospeciation_step_jSpinner;
    private javax.swing.JSpinner cospeciation_upper_bound_jSpinner;
    private javax.swing.JButton default_button;
    private javax.swing.JSpinner duplication_host_switch_jSpinner_SetCosts;
    private javax.swing.JLabel duplication_host_switch_label_rangecosts;
    private javax.swing.JLabel duplication_host_switch_label_setcosts;
    private javax.swing.JSpinner duplication_host_switch_lower_bound_jSpinner;
    private javax.swing.JSpinner duplication_host_switch_number_of_steps_jSpinner;
    private javax.swing.JSpinner duplication_host_switch_step_jSpinner;
    private javax.swing.JSpinner duplication_host_switch_upper_bound_jSpinner;
    private javax.swing.JSpinner duplication_jSpinner_SetCosts;
    private javax.swing.JLabel duplication_label_rangecosts;
    private javax.swing.JLabel duplication_label_setcosts;
    private javax.swing.JSpinner duplication_lower_bound_jSpinner;
    private javax.swing.JSpinner duplication_number_of_steps_jSpinner;
    private javax.swing.JSpinner duplication_step_jSpinner;
    private javax.swing.JSpinner duplication_upper_bound_jSpinner;
    private javax.swing.JSpinner failure_to_diverge_jSpinner_SetCosts;
    private javax.swing.JLabel failure_to_diverge_label_rangecosts;
    private javax.swing.JLabel failure_to_diverge_label_setcosts;
    private javax.swing.JSpinner failure_to_diverge_lower_bound_jSpinner;
    private javax.swing.JSpinner failure_to_diverge_number_of_steps_jSpinner;
    private javax.swing.JSpinner failure_to_diverge_step_jSpinner;
    private javax.swing.JSpinner failure_to_diverge_upper_bound_jSpinner;
    private javax.swing.JLabel from_label;
    private javax.swing.JSpinner infestation_jSpinner_SetCosts;
    private javax.swing.JLabel infestation_label_rangecosts;
    private javax.swing.JLabel infestation_label_setcosts;
    private javax.swing.JSpinner infestation_lower_bound_jSpinner;
    private javax.swing.JSpinner infestation_number_of_steps_jSpinner;
    private javax.swing.JSpinner infestation_step_jSpinner;
    private javax.swing.JSpinner infestation_upper_bound_jSpinner;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSpinner loss_jSpinner_SetCosts;
    private javax.swing.JLabel loss_label_rangecosts;
    private javax.swing.JLabel loss_label_setcosts;
    private javax.swing.JSpinner loss_lower_bound_jSpinner;
    private javax.swing.JSpinner loss_number_of_steps_jSpinner;
    private javax.swing.JSpinner loss_step_jSpinner;
    private javax.swing.JSpinner loss_upper_bound_jSpinner;
    private javax.swing.JLabel number_of_steps_label;
    private javax.swing.JButton ok_button;
    private javax.swing.JPanel region_costs_jPanel;
    private javax.swing.JLabel region_costs_label;
    private javax.swing.JTable region_costs_table;
    private javax.swing.JLabel step_label;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JLabel to_label;
    // End of variables declaration//GEN-END:variables
}
