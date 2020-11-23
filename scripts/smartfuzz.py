"""
=====================
This is an automated pair-wise testing tool for SmartThings Apps
@author: lkshar
=====================
Run command: 
$ source apsecenv/bin/activate
$ python smartfuzz.py <Your Smart App Name> <Your Samsung Account> <Your Samsung Account Password> 
"""
print(__doc__)
import os, sys
import datetime
import gzip, shutil
import requests
import utility
import logging
from itertools import chain, combinations, product
from allpairspy import AllPairs
from random import randint

import pytest
import time
import json
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support import expected_conditions
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
from selenium.webdriver.support import expected_conditions as EC
  
# extract input values in Preferences/sections
def analyze_preferences():
    # get Preferences section
    elem = driver.find_element(By.ID, "ide-preferences-panel")
    # each user input is under a section class
    sections = elem.find_elements(By.CLASS_NAME, "section")
    
    print("num of inputs/sections " + str(len(sections)))
    for section in sections:
        if skip_device_selection:
            # check if the section is for selecting devices
            # if yes, skip. Not fuzzing devices. Use the default selection by the tester
            device_section = section.find_elements_by_class_name("virtual") # a list of devices
            if device_section:
                print("skip device section")
                handle_radio(section)
                continue

        # input elements are usually inside class input and tray
        # this code assumes that each input class has only one tray div element
        input_classes = section.find_elements_by_class_name("input") # a list of inputs
        for input_class in input_classes:
           
            tray = input_class.find_elements_by_class_name("tray")[0]
         
            # <input .. > and <select ..><option>..</select>
            input_tags = tray.find_elements(By.TAG_NAME, "input")
            select_tags = tray.find_elements(By.TAG_NAME, "select")
            
            for elem in input_tags:
                analyze_type_input(elem) # <input type="xxx">
                
            for elem in select_tags:
                # <select class="xxx"><option value="motion:on"></option><option ... </select>
                analyze_type_select(elem) 

def handle_radio(section): # work around to handle radio type virtual device selection
    input_classes = section.find_elements_by_class_name("input") # a list of inputs
    for input_class in input_classes:
        tray = input_class.find_elements_by_class_name("tray")[0]
        input_tags = tray.find_elements(By.TAG_NAME, "input")
        for input_tag in input_tags:
            input_type = input_tag.get_attribute("type")
   
            if input_type == "radio": #or input_type == "button" or input_type == "reset" or input_type == "submit":
                print("Radio button detected in Device section!")
                arrow_classes = section.find_elements_by_class_name("arrow")
                for arrow in arrow_classes:
                    arrow.click()
                    time.sleep(5)

                element = WebDriverWait(driver, 10).until(
                    EC.presence_of_element_located((By.CLASS_NAME, "tray"))
                )
                tray = input_class.find_elements_by_class_name("tray")[0]
                try:
                    tray.click() 
                    time.sleep(3)
                    input_tag.click()
                    time.sleep(3)
                except:
                    print("Radio button can't click. Proceed")              

# analyze <input type='xxxx'> element
def analyze_type_input(elem):
    input_type = elem.get_attribute("type")
    # assume tester provids a default value
    # but even if tester does not provide, the tool will still work and select values from pool
    default_value = elem.get_attribute("value") 
    param = elem.get_attribute("id")
    
    print("Param_Name: %s" % param)
    print("Default value: %s" % default_value)
   
    if input_type == "checkbox" or input_type == "radio" or input_type == "button" or input_type == "reset" or input_type == "submit":
        values = ["check", "uncheck"] # for this input type, there is only "check" and "uncheck"
        
    elif input_type == "range":
        min_val = elem.get_attribute("min")
        max_val = elem.get_attribute("max")
        values = [min_val, max_val, default_value]

    else:
        if input_type != "email" or input_type != "date" or input_type != "time" or input_type != "tel" or input_type != "url":
            input_type = "" # for the rest of input_types, either it is defined from (csv) data file or default to String/Text

        curr_values = []
        if param in params:
            idx = params.index(param)
            curr_values = param_values[idx]
        if not curr_values: # if curr_values is an empty list
            default_params.append(param)

        values = [default_value] # for the rest of input_types, only the default value is available to us
        
    add_to_Params(param, values, input_type)     
   

# analyze <select> element
def analyze_type_select(elem):
    
    param = elem.get_attribute("id")
    print("Param: %s" % param)
    
    all_options = elem.find_elements_by_tag_name("option")
    values = []
    for option in all_options:
        val = option.get_attribute("value")
        print("Option Value: %s" % val)
        values.append(val)

    add_to_Params(param, values)

# analyze device section, e.g. swtich on/off, motion sensor active/inactive, etc.
def analyze_simulator():
    # install/deploy the app
    install_elem = wait.until(EC.element_to_be_clickable((By.ID, "update")))
    install_elem.click()
    time.sleep(5)

    # get simulator panel <div>
    panel = driver.find_element(By.ID, "ide-simulator-panel")
    # devices <select> and <button> elements are under 'status' class
    devices = panel.find_elements(By.CLASS_NAME, "status")
    # device labels are under 'deviceLabel' class
    labels = panel.find_elements(By.CLASS_NAME, "deviceLabel")
    # labels = panel.find_elements(By.CLASS_NAME, "title") # alternative way if the above doesn't work
    
    for i,device in enumerate(devices):
        dropdown = device.find_elements(By.CLASS_NAME, "form-control")[0]
        param = labels[i].text
        #param = labels[i].get_attribute("title") # alternative way if the above doesn't work
        print("Param: %s" % param)
        all_options = dropdown.find_elements_by_tag_name("option")
        values = []
        for option in all_options:
            val = option.get_attribute("value")
            print("Option Value: %s" % val)
            # following is the workaround to handle IDE's predefined device values...
            if ":" in val: # e.g. switch:on, motion:active -> on, active
                val = val.split(":")[1].strip() # remove switch:, motion:

            values.append(val)

        add_to_Params(param, values, "capability")
        #time.sleep(2)

def setup():
    # initialize and log in
    driver = webdriver.Chrome()
    """ An implicit wait tells WebDriver to poll the DOM for a certain amount of time 
        when trying to find any element (or elements) not immediately available. 
        Once set, the implicit wait is set for the life of the WebDriver object. """
    driver.implicitly_wait(10)
    try:
        driver.get(url)
        #driver.set_window_size(1529, 1279)
        driver.find_element(By.LINK_TEXT, "Log in").click()
        driver.find_element(By.CSS_SELECTOR, ".sa-login-btn").click()
        driver.find_element(By.ID, "iptLgnPlnID").send_keys(user)
        time.sleep(2)
        driver.find_element(By.ID, "iptLgnPlnPD").send_keys(pwd)
        time.sleep(2)
        driver.find_element(By.ID, "signInButton").click()
        driver.find_element(By.LINK_TEXT, "My SmartApps").click()
        driver.find_element(By.LINK_TEXT, appName).click()
        time.sleep(5)
        driver.find_element(By.CSS_SELECTOR, ".run-btn").click()
        time.sleep(5)
        driver.find_element(By.CSS_SELECTOR, ".btn-success").click()
        time.sleep(5)
        return driver
    except:
        driver.quit()
        return None

def analyze_dataFile():
    try:
        with open(dataFile, 'r') as f:
            for i, line in enumerate(f):
                #print(line)
                if i == 0: # skip first line
                    continue 
                tokens = line.split(",")  # expected: 4 tokens
                #print("num of tokens: " + str(len(tokens)))
                param = tokens[0] # first one is the variable name
                method = tokens[1].lower() # second one is the method in which the variable is found
                dtype = tokens[2].lower() # third one is the data type 
                values = tokens[3].split() # fourth one is the values

                # only preferences and endpoints are simulatable parameters
                if "preferences" in method or "endpoint" in method:
                    add_to_Params(param, values, dtype)
                elif "sink" in param:
                    sinks["num_sinks"] = int(values[0].strip())  
                else: 
                    # the values of the rest of the parameter are added into pool
                    for val in values:
                        pool.append(val)    
        f.close()
    except:
        print("No data (csv) file or err in parsing file")
  
def add_to_Params(param, values, dtype=""):
    if dtype != "": 
        params_dtype[param] = dtype
        if "endpoint" in dtype:
            if not values: # if there is no value, assign an empty string item
                values = [""]
        if "capability" in dtype or "endpoint" in dtype:
            if param not in event_params:
                event_params.append(param)

    for val in values:
        pool.append(val)

    if param in params:
        idx = params.index(param)
        curr_values = param_values[idx]
        new_values = curr_values + values
        new_values = list( dict.fromkeys(new_values) ) # remove duplicate values
        param_values[idx] = new_values
    else:
        params.append(param)
        param_values.append(values)

def run_analysis(pairs):
    # this simulates input values under Preferences. E.g. devices, Mode, time, etc. 
    simulate_inputs(pairs)
    time.sleep(5)
    
    # install/deploy the app
    install_elem = wait.until(EC.element_to_be_clickable((By.ID, "update")))
    install_elem.click()
    time.sleep(5)
    wait.until(EC.presence_of_element_located((By.ID, "ide-simulator-panel"))) # wait till installed done and simulator panel available
    # simulate device events and endpoint events
    simulate_events(pairs)
    time.sleep(3)
    
# simulate input values in Preferences/sections
def simulate_inputs(pairs):
    # get Preferences section
    elem = driver.find_element(By.ID, "ide-preferences-panel")
    # each user input is under a section class
    sections = elem.find_elements(By.CLASS_NAME, "section")

    print("num of inputs/sections " + str(len(sections)))
    for section in sections:
        if skip_device_selection:
            # check if the section is for selecting devices
            # if yes, skip. Not fuzzing devices
            device_section = section.find_elements_by_class_name("virtual")
            if device_section:
                print("skip device section")
                handle_radio(section)
                continue

        arrow_classes = section.find_elements_by_class_name("arrow")
        for arrow in arrow_classes:
            arrow.click()
            time.sleep(5)

        # input elements are usually inside class input and tray
        # this code assumes that each input class has only one tray div element
        element = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.CLASS_NAME, "input"))
            )
        input_classes = section.find_elements_by_class_name("input")
        device_selections = section.find_elements_by_class_name("device-selections")
        for i,input_class in enumerate(input_classes):
            try: 
                device_selections[i].click()
            except:
                input_class.click()  
            time.sleep(3)
            
            tray = input_class.find_elements_by_class_name("tray")[0]
            element = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.CLASS_NAME, "tray"))
            )
            try:
                tray.click()  
            except:
                print("try can't click. Proceed")

            time.sleep(3)
            
            # <input .. > and <select ..><option>..</select>
            input_tags = tray.find_elements(By.TAG_NAME, "input")
            select_tags = tray.find_elements(By.TAG_NAME, "select")

            element = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.TAG_NAME, "input"))
            )

            element = WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.TAG_NAME, "select"))
            )
            try: 
                for elem in input_tags:
                    process_input(elem,pairs)
                    
                for elem in select_tags:
                    process_select(elem,pairs)
            except:
                print("input element can't select/click. Proceed!")

# check the type of <input type='text'> element and perform corresponding action: click(), sendkey("value")
def process_input(elem,pairs):
    input_type = elem.get_attribute("type")
    value = elem.get_attribute("value")
    param = elem.get_attribute("id")

    if param in params:
        idx = params.index(param)
        value = pairs[idx] # the value comes from pairwise test gen

        if param in default_params: # randomly get a different value for params assigned with default values initially
            if utility.change():
                value = utility.generate_a_value(params,param_values,pool,param,input_type)
    
    else:
        value = utility.generate_a_value(params,param_values,pool,param,input_type)
        print("{} not in params list".format(param))

    print("Param: %s" % param)
    print("Generated value: %s" % value)
    logging.info("Param: %s" % param)
    logging.info("Generated value: %s" % value)
    
    if input_type == "checkbox" or input_type == "radio" or input_type == "button" or input_type == "reset" or input_type == "submit":
        if value != "uncheck":
            elem.click()
    elif input_type == "file":
        print("Input File type encountered!")
        logging.info("Input File type encountered. We don't handle it yet!")
        return # currently we don't deal with file input type # TODO check if there is such a case
    else:
        try:
            elem.clear()
            elem.send_keys(value)
        except:
            print("Elem can't clear. Proceed!")  
        
    time.sleep(3)

# perform action for <select> element
def process_select(elem,pairs):
    elem.click()
    param = elem.get_attribute("id")
    value = ""
    if param in params:
        idx = params.index(param)
        value = pairs[idx]
    else:
        print("param not found")
    
    print("Param: %s" % param)
    print("Generated value: %s" % value)
    logging.info("Param: %s" % param)
    logging.info("Generated value: %s" % value)
   
    all_options = elem.find_elements_by_tag_name("option")
    for option in all_options:
        op_val =  option.get_attribute("value")
        #print("Option Value: %s" % op_val)
        
        if value == op_val:
            option.click()  # randomly select an option
            break
        if value == "":
            print("Bug: somehow this <select> param is not in our params list")
            if utility.change():
                option.click()  # randomly select an option
                break

# this simulates device and endpoint events, e.g. swtich on/off, motion sensor active/inactive, etc.
def simulate_events(pairs):
    permutation = []
    num_evt_params = len(event_params)
    rand_indices = utility.generate_random_range(0,num_evt_params)

    for rand_idx in rand_indices:
        param = event_params[rand_idx]
        permutation.append(param)
        idx = params.index(param)
        value = pairs[idx]  # get corresponding value for the param, from pairs
        dtype = params_dtype[param]
        
        record_event('event', param + ' ' + value) # send event info to our backend server
        
        if "capability" in dtype:
            generate_device_event(param,value)
        elif "endpoint" in dtype:
            generate_endpoint_event(param,value)

        time.sleep(wait_after_event)  # wait xx secs to take effect after every event generation

    tests["permutations"].append(permutation)

def simulate_permutated_events(pairs):
    num_evt_params = len(event_params)
    # generate permutations without repetition for the event (device commands and endpoints) parameters
    uniq_permutations = utility.unique_permutations(event_params, num_evt_params)
   
    for i, permutation in enumerate(uniq_permutations):
        tests["num_testcases"] += 1
        record_event("Test Case" , str(tests["num_testcases"]))
    
        print("Permutation " + str(i+1) + "...")
        for param in permutation:
            duration = round((time.time() - start_time)/3600,3)
            if duration > tests["timeout"]:
                tests["isTimeout"] = True
                return
                
            idx = params.index(param)
            value = pairs[idx]  # get corresponding value for the param, from pairs
            dtype = params_dtype[param]
        
            record_event('event', param + ' ' + value) # send event info to our backend server
            
            if "capability" in dtype:
                generate_device_event(param,value)
            elif "endpoint" in dtype:
                generate_endpoint_event(param,value)

            time.sleep(wait_after_event)  # wait xx secs to take effect after every event generation

        checkSinksCovered()
        if tests["isAllSinksCovered"]:
            return
        

def generate_device_event(param, value):
    print("Generating device event...")
    logging.info("Generating device event...")
    try:
        # get simulator panel <div>
        panel = driver.find_element(By.ID, "ide-simulator-panel")
        # devices are under 'status' class
        devices = panel.find_elements(By.CLASS_NAME, "status")
        # device labels are under 'deviceLabel' class
        labels = panel.find_elements(By.CLASS_NAME, "deviceLabel")
        # labels = panel.find_elements(By.CLASS_NAME, "title") # alternative way if the above doesn't work
        
        dev_idx = 0
        print("Param: %s" % param)
        for i,label in enumerate(labels):
            print("DeviceLabel: %s" % label.text)
            if label.text == param:
                dev_idx = i
                break

        dropdown = devices[dev_idx].find_elements(By.CLASS_NAME, "form-control")[0]
        dropdown.click()
        print("Generated value: %s" % value)
        logging.info("Param: %s" % param)
        logging.info("Generated value: %s" % value)
    
        all_options = dropdown.find_elements_by_tag_name("option")
        for option in all_options:
            op_val =  option.get_attribute("value")
            print("Option Value: %s" % op_val)
            
            if value in op_val:
                print("Device command clicking....")
                option.click()  # randomly select an option
                break
            if value == "": # bug: somehow this <select> param has no pre-selected value
                if utility.change():
                    option.click()  # randomly select an option
                    break

        # click btn after select
        devices[dev_idx].find_element(By.CLASS_NAME, "btn").click()
        time.sleep(5)
    except:
        # this happens when IDE is unable to setup the simulator section properly
        # TODO: optionally we can inject equiv endpoints in the app and use generate_endpoint_event() to generate the same event
        print("Err: Unable to generate device event")
        logging.info("Err: Unable to generate device event")

def generate_endpoint_event(param,value):
    print("Generating endpoint event...")
    ok = False
    try:
        # this extracts API token and endpoint values from the IDE
        token, endpoint = utility.get_token_endpoint(driver)
        
        if value == "":
            ep_url = endpoint + '/' + param
        else:
            ep_url = endpoint + '/' + param + '/' + value
        logging.info("Generating endpoint event... \n" +  ep_url)
        ok = utility.connect_endpoint(ep_url, token)
    except:
        if not ok:
            print("Err: Unable to generate endpoint event")
            logging.info("Err: Unable to generate endpoint event")
    
def run_permutation_analysis(pairs):
    print("Run Permutation analysis!")
     # this simulates input values under Preferences. E.g. devices, Mode, time, etc. 
    simulate_inputs(pairs)
    time.sleep(5)
    
    # install/deploy the app
    install_elem = wait.until(EC.element_to_be_clickable((By.ID, "update")))
    install_elem.click()
    time.sleep(5)
    wait.until(EC.presence_of_element_located((By.ID, "ide-simulator-panel"))) # wait till installed done and simulator panel available

    # simulate device events and endpoint events
    simulate_permutated_events(pairs)
    time.sleep(3)
    
def checkSinksCovered():
    try:
        # this logic is not very efficient. It checks all the lines, including those that have been checked previously
        with open(things, 'r') as f:
            for _, line in enumerate(f):
                if "action" in line:
                    tokens = line.split(":")  # expected: 3 tokens
                    sink = tokens[1].strip()  # e.g. sink1
                    if not sink in sinks["sinks_covered"]:
                        sinks["sinks_covered"].append(sink)
                        sinks["num_sinks_covered"] += 1
        f.close()
    except:
        print("Err: opening test output file")

    if sinks["num_sinks_covered"] >= sinks["num_sinks"]:
            tests["isAllSinksCovered"] = True
    
def record_event(param,value): # send event information to our backend server
    print("Sending info to server...")
    # syntax: requests.get(url, params={key: value}, args)
    params = { param: value }
    logging.info('Sending info to server: ' + param + ' ' + value)
    try:
        response = requests.get(backend_url,params)
        if not str(response.status_code).startswith('2'):
            print("Err: Unable to send info to server")
    except:
            print("Excep: Unable to send info to server")

def print_parameters(): # just print results of parameters analysis
    print("Number of sinks: " + str(sinks["num_sinks"]))
    print("Simulatable parameter : dtype : values")
    logging.info("Simulatable parameter : dtype : values")
    for i, val in enumerate(param_values):
        param = params[i]
        try:
            dtype = params_dtype[param]
        except:
            params_dtype[param] = "" # this will be treated as string
            dtype = ""
        print("{} : {} : {}".format(param, dtype, val))
        logging.info("{} : {} : {}".format(param, dtype, val))

    print("Default parameters")
    for val in default_params:
        print("{}".format(val))

    print("Pool values")
    for val in pool:
        print("{}".format(val))

def log_duration_sinkscovered(duration): # log duration and sink covered after running each test case
    logging.info("Duration: {}".format(duration))
    logging.info("Sinks covered: {}".format(sinks["num_sinks_covered"]))
    record_event("Duration", str(duration))
    record_event("Sinks_covered", str(sinks["num_sinks_covered"]))

    print("Duration: {}".format(duration))
    print("Sinks covered: {}".format(sinks["num_sinks_covered"]))

def log_results():  # log test results   
    num_simulatable_params = len(params)
    logging.info("Number of parameters: " + str(num_simulatable_params))
    logging.info("Number of pairwise test cases: " + str(tests["num_pairwise_testcases"]))
    logging.info("Total Number of test cases: " + str(tests["num_testcases"]))
    logging.info("Number of sinks: " + str(sinks["num_sinks"]))
    logging.info("Number of sinks covered by pairwise tests: " + str(len(sinks["sinks_covered_pairwise"])))
    logging.info("Number of sinks covered: " + str(sinks["num_sinks_covered"]))
   
    logging.info("Sinks covered by pairwise tests: {}".format(sinks["sinks_covered_pairwise"]))
    logging.info("Sinks covered: {}".format(sinks["sinks_covered"]))
    logging.info("\nDuration: {:.2f} hours".format((time.time() - start_time)/3600))
    logging.info(str(datetime.datetime.now()) + '\n\n')

    print("Number of parameters: " + str(num_simulatable_params))
    print("Number of pairwise test cases: " + str(tests["num_pairwise_testcases"]))
    print("Total Number of test cases: " + str(tests["num_testcases"]))
    print("Number of sinks: " + str(sinks["num_sinks"]))
    print("Number of sinks covered by pairwise tests: " + str(len(sinks["sinks_covered_pairwise"])))
    print("Number of sinks covered: " + str(sinks["num_sinks_covered"]))
   
    print("Sinks covered by pairwise tests: {}".format(sinks["sinks_covered_pairwise"]))
    print("Sinks covered: {}".format(sinks["sinks_covered"]))
    print("\nDuration: {:.2f} hours".format((time.time() - start_time)/3600))
    print(str(datetime.datetime.now()) + '\n\n')
    
if __name__ == '__main__':
    start_time = time.time()
       
    # app under test
    try:
        appName = sys.argv[1]  # use \ if there are spaces in the app name, e.g. jesper\ :\ VPS_APP
    except:
        appName = 'jesper : f-drythewetspot_ins'
       
    # set num of seconds to wait
    try:
        wait_after_event = int(sys.argv[2])  
    except:
        wait_after_event = 60 # this sets how long the tool waits after every event generation  

     # credentials
    try:
        user = sys.argv[3]  
    except:
        user = '<Your Samsung Account>'  
       
    try:
        pwd = sys.argv[4]  
    except:
        pwd = '<Your Samsung Account Password>'
      
    
    url = 'https://graph.api.smartthings.com/'  # backend url where test outputs are sent (via code instrumentation) 
    backend_url = 'https://7a09f96c0e1c.ngrok.io/fuzzthings/things.php'
    things = '/Applications/MAMP/htdocs/fuzzthings/things.txt'  # file in which test outputs are logged
    skip_device_selection = True # this sets if device selection is to be simulated or fixed (use default selection by IDE)

    logging.basicConfig(filename='app.log', filemode='a', format='%(message)s', level=logging.INFO)
    logging.info("Analyzing app: " + appName)
    record_event("App", appName)

    print("===========Analyzing App: " + appName + "===============")

    # initialize parameters for pair-wise and permutation testing
    sinks = { "num_sinks" : 0, "num_sinks_covered" : 0, "sinks_covered_pairwise" : [], "sinks_covered" : [] }
    tests = { "num_testcases" : 0, "num_pairwise_testcases" : 0, "permutations" : [], "timeout" : 3, "isTimeout" : False, "isAllSinksCovered" : False}
   
    pool = [] # a pool of literal values extracted from static analysis of the app. Use for assigning to inputs randomly
    params = [] # simulatable parameters: [ "lock", "motion", "mode", "falseAlarmThreshold" ]
    event_params = [] # a subset of simulatable parameters -- for simulating device and endpoint events
    params_dtype = {} # (inferred) data types of params { "locks": "capability.lock", "falseAlarmThreshold": "decimal" } 
    param_values = [] # [ ["lock", "unlock"], ["on", "off"], ["Away, "1", "2"] ]
    default_params = [] # parameters that are provided with default values -- those params may need to be assigned with pool
    
    dataFile = appName.split(":")[1].strip().lower() + ".csv" # darkenbehindme_ins.csv
    dataFile = os.path.join("data", dataFile)
    
    # analyze data file produced by static analysis (contexIoT tool)
    # print(dataFile)
    analyze_dataFile()
    
    # analyze IDE's simulator section and extract device capability/commands
    for n in range(3):
        driver = setup()
        if driver != None: # driver setup ok
            break
    wait = WebDriverWait(driver, 60)
    analyze_preferences()
    analyze_simulator()
    driver.quit()
    # post processing of parameters
    for i,values in enumerate(param_values):
        if not values: # if there is no value, give an empty string
            param_values[i] = [""]
    pool = list( dict.fromkeys(pool) ) # remove duplicate values
    print_parameters()

    # Pairwise tests
    pairwise = AllPairs(param_values)
    duration = 0
    for i, pairs in enumerate(pairwise): 
        tests["num_testcases"] += 1
        record_event("Test Case" , str(tests["num_testcases"]))
        logging.info(" ===== Running test case {} ===== ".format(tests["num_testcases"]))
        print(" ===== Running test case {} ===== ".format(tests["num_testcases"]))
        # driver setup
        for n in range(3):
            driver = setup()
            if driver != None:
                break
        if driver == None: # if fails for 3 times, stop
            continue  
        wait = WebDriverWait(driver, 20)
        driver.implicitly_wait(20) # seconds
        run_analysis(pairs)
        driver.quit()

        checkSinksCovered()
        duration = round((time.time() - start_time)/3600,3) # round to 3 decimal places
        log_duration_sinkscovered(duration)
        if duration > tests["timeout"]:
            tests["isTimeout"] = True
            print("Timeout")
            logging.info("Timeout")
            break
        if tests["isAllSinksCovered"]:
            print("All sinks covered!")
            logging.info("All sinks covered")
            break

    tests["num_pairwise_testcases"] = tests["num_testcases"]
    print("Number of pairwise test cases: " + str(tests["num_pairwise_testcases"]))
    
    # check for sinks coverage. If not covered all, run permutation testing
    checkSinksCovered()
    sinks["sinks_covered_pairwise"] = sinks["sinks_covered"][:]  # copy by value [:]
    if tests["isTimeout"]:  
        print("Timeout")
        logging.info("Timeout")
    elif tests["isAllSinksCovered"]:
        print("All sinks covered!")
        logging.info("All sinks covered")
    else: # not tests["isAllSinksCovered"]
        print('Pairwise test cases did not cover all sinks. Using permutations!')
        new_pairwise = AllPairs(param_values)
        for i, pairs in enumerate(new_pairwise):
            for n in range(3):
                driver = setup()
                if driver != None:
                    break
            if driver == None: # if fails for 3 times, stop
                continue  
            wait = WebDriverWait(driver, 20)
            driver.implicitly_wait(20) 
            run_permutation_analysis(pairs)
            driver.quit()

            duration = round((time.time() - start_time)/3600,3) # round to 3 decimal places
            log_duration_sinkscovered(duration)
            if duration > tests["timeout"]:
                tests["isTimeout"] = True
                print("Timeout")
                logging.info("Timeout")
                break
            if tests["isAllSinksCovered"]:
                print("All sinks covered!")
                logging.info("All sinks covered")
                break
         
    log_results()
  