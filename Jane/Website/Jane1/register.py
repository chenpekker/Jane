#!/usr/bin/python2.5

import time
import cgi

FORM=cgi.FieldStorage()

#REGISTER_FORM_FILE = 'form.html'
DOWNLOAD_PAGE = 'download.html'

NAME_KEY = 'name'
EMAIL_KEY = 'email'
INST_KEY = 'inst'

USERS_FILE = 'users.txt'
DELIM_CHAR1 = ';'
DELIM_CHAR2 = "\n"

#def print_form():
#    print "Content-type: text/html\n\n"
#    print open(REGISTER_FORM_FILE).read()

def print_dl_page():
    print "Content-type: text/html\n\n"
    print open(DOWNLOAD_PAGE, 'r').read()

def form_is_filled():
    if not (FORM.has_key(NAME_KEY) \
    and     FORM.has_key(EMAIL_KEY) \
    and     FORM.has_key(INST_KEY)):
        return False

    return      FORM[NAME_KEY] != '' \
            and FORM[EMAIL_KEY] != '' \
            and FORM[INST_KEY] != ''

def clean(s):
    s = '?'.join(s.split(DELIM_CHAR1))
    return '?'.join(s.split(DELIM_CHAR2))

def store_user():
    if form_is_filled():
        name = FORM[NAME_KEY].value
        email = FORM[EMAIL_KEY].value
        inst = FORM[INST_KEY].value

        name = clean(name)
        email = clean(email)
        inst = clean(inst)

    else:
        name    = '?'
        email   = '?'
        inst    = '?'

    entry = DELIM_CHAR1.join([str(time.time()), name, email, inst]) 
    file = open(USERS_FILE, 'a')
    file.write(entry + DELIM_CHAR2)
    file.close()

def main():
#    if form_is_filled():
#        print_form()
#        store_user()
#    else:
    store_user()
    print_dl_page()

if __name__ == "__main__":
    main()
#    import os
#    os.system('env')


