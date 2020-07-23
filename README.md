# Fuzzing SmartThings Apps
Software Artefacts used in APSEC20 work

instrumented_apps:
    
    contain 60 instrumented apps used in the experiments

scripts:
    
    apsecenv: Python dependencies
    chromedriver: web driver used for Selenium testing
    randFuzz.py -- random fuzzing tool
    smartFuzz.py -- smart fuzzing tool
   
   Run command:
    
    $ source apsecenv/bin/activate
    $ python randfuzz.py <Your Smart App Name> <Your Samsung Account> <Your Samsung Account Password>

sinkAPIs: 
    
    a set of APIs corresponding to sensitive operations
