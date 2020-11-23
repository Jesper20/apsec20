"""
=====================
This is a utility python module for smartfuzz.py and randomfuzz.py
@author: lkshar
=====================
"""

import requests
import time
import datetime
import string
import random
from selenium import webdriver
from selenium.webdriver.common.by import By
from itertools import permutations

rand = False
modes = ["Home", "Away", "Night", "sunset", "sunrise"] # add possible modes
data_types = { 
                # following are inputs from preferences
                "string" : ["string", "text", ""],
                "number" : ["int", "long", "number"],
                "decimal" : ["decimal", "float", "double"],
                "boolean" : ["bool", "Boolean", "boolean"],
                "check" : ["checkbox", "radio", "button", "reset", "submit"],
                "range" : ["range"],
                "email" : ["email"],
                "date" : ["date"],
                "time" : ["time"],
                "phone" : ["tel", "phone"],
                "url" : ["url"],
                # following are device events and endpoints
                "capability" : ["capability"],
                "mode" : ["mode", "location"],
                "endpoint" : ["endpoint"]
            }
######################## utility functions ##########################
def setRand():
    global rand
    rand = True

def unique_permutations(iterable, r=None):
    previous = tuple()
    for p in permutations(sorted(iterable), r):
        if p > previous:
            previous = p
            yield p

def generate_a_value(params, param_values, pool, param, dtype=""): # attempt to generate a valid value for a given data type and the pool
    dtype = get_key(dtype, data_types)
    value = ""
    ch = change() # 0.5 probability of changing to a random value, instead of the one from pool

    if pool:
        i = len(pool) - 1
        rand_idx = random.randint(0, i)
        value = pool[rand_idx]

    if dtype == "string":
        if ch:
            value = generate_random_string()

    elif dtype == "boolean":
        value = random.choice(["true", "false"])

    elif dtype == 'mode':
        if not valid_mode(value) or ch:
            value = random.choice(modes) 

    elif dtype == "check":
        value = random.choice(["check", "uncheck"])

    elif dtype == "number":
        if not valid_number(value) or ch:
            value = random.randint(-10, 100)
      
    elif dtype == 'decimal':
        if not valid_decimal(value) or ch:
            value = generate_random_decimal()
            
    elif dtype == "range":
        value = generate_random_number()
        if param in params:
            try:
                idx = params.index(param)
                min_val = param_values[idx][0] # idx 0 stores min val
                max_val = param_values[idx][1] # idx 1 stores max val
                value = random.randint(min_val, max_val)
            except:
                value = value

    elif dtype == "email": # if change is true or if the selected value from pool is invalid, generate a valid value
        if ch or not valid_email(value): # check if curr value from pool is a valid email
            value = generate_random_email()  
    elif dtype == "date":
        if ch or not valid_date(value):
            value = generate_random_date()
    elif dtype == "time":
        if ch or not valid_time(value):
            value = generate_random_time()
            #print("time value: " + str(value))
    elif dtype == "phone":
        if ch or not valid_phone(value):
            value = generate_random_phone()
    elif dtype == "url":
        if ch or not valid_url(value):
            value = generate_random_url()

    #TODO: to handle more data types, as necessary
          
    return value

def generate_random_string(stringLength=8):
    #letters = string.ascii_lowercase  # only lower case
    letters = string.ascii_letters # both lower case and upper case
    return ''.join(random.choice(letters) for i in range(stringLength))

def generate_random_number(stringLength=2):
    letters = string.digits # both digits
    if rand: # if rand mode is on, increase the range
        stringLength = 4
    return ''.join(random.choice(letters) for i in range(stringLength))

def generate_random_decimal():
    value = 0.001
    if rand:
        value = random.random()
    return value

def generate_random_alphaNumeric_string(stringLength=8):
    lettersAndDigits = string.ascii_letters + string.digits
    return ''.join((random.choice(lettersAndDigits) for i in range(stringLength)))

# generate a list of values between the two given range r1 and r2 (r2 exclusive), randomly ordered
def generate_random_range(r1, r2):
    result = list(range(r1, r2))
    random.shuffle(result)
    #print(result)
    return result

# function to return key for any value from a given dictionary
def get_key(val, my_dict): 
	for key, values in my_dict.items(): 
		if val in values: # values is a list 
			return key 
	return "string"  # default dtype

def change():
    # seed random number generator
    # seed(1) # this always results in the same random value
    change = False 
    prob = random.random() # random val btw 0 to 1
    #print(prob)
    if prob < 0.5: # prob of change < 0.5
        change = True

    if rand: # if this mode is on, always change value
        change = True
    return change

def get_token_endpoint(driver):
    token_elem = driver.find_element(By.ID, "api-client-id")
    endpoint_elem = driver.find_element(By.ID, "api-endpoint")

    token = token_elem.get_attribute("value")
    endpoint = endpoint_elem.get_attribute("value")

    print("API Token: " + token)
    print("API Endpoint: " + endpoint)
    return token, endpoint

def connect_endpoint(ep_url,token,method='get',data=None):
    headers = {'Authorization' : 'Bearer ' + token }
  
    print(ep_url)
    # data = {key: value}  # data is a dictionary
    if method.lower() == 'post':
        print("POST request")
        if data != None:
            response = requests.post(ep_url, data, headers=headers)
        else:
            response = requests.post(ep_url, headers=headers)
    elif method.lower() == 'put':
        print("PUT request")
        if data != None:
            response = requests.post(ep_url, data, headers=headers)
        else:
            response = requests.post(ep_url, headers=headers)
    else:
        print("GET request")
        if data != None:
            response = requests.get(ep_url, data, headers=headers)
        else:
            response = requests.get(ep_url, headers=headers)
      

    print(str(response.status_code))
    #print(response.json())
    #x = json.loads(response)
    #print(str(x))
    if str(response.status_code).startswith('2'): # success
        return True
    else: # error
        return False


def valid_number(value):
    valid = False
    try: # attempt to cast value to number dtype usign try-except-finally block
        value = int(value)
        return valid
    except: 
        return valid
    return valid

def valid_mode(value):
    valid = False
    if value in modes:
        valid = True
    return valid

# TODO: to implement the following, remaining methods....
def valid_decimal(value):
    valid = False
    return valid

def valid_email(value):
    valid = False

    return valid

def valid_date(value):
    valid = False

    return valid

def valid_time(value):
    valid = False

    return valid

def valid_phone(value):
    valid = False

    return valid

def valid_url(value):
    valid = False

    return valid

def generate_random_email():
    value = "user@email.com"
    return value

def generate_random_date():
    ts = time.time()
    value = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d')
    return value

def generate_random_time():
    ts = time.time()
    value = datetime.datetime.fromtimestamp(ts).strftime('%H:%M') # get current time
    if rand: # in rand mode, randomise hour value
        tokens = value.split(":")
        r = random.randint(-10,10)
        val1 = int(tokens[0]) + r
        val2 = tokens[1]
        value = str(val1) + ":" + str(val2)   
    return value

def generate_random_phone():
    value = "11111111"
    return value

def generate_random_url():
    value = "https://7a09f96c0e1c.ngrok.io/fuzzthings/things.php"
    return value

