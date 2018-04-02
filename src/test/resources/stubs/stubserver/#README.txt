To use the stubs in the ../stubdata directory

a) Start the stub server by running runstubs.cmd
b) You can check that the appropriate stubs have been picked up (as defined in the stubs.yaml file)
   by looking at the Stubby4j admin portal at http://localhost:8889/status
   
Note that the JUnit based integration tests automatically start an instance of Stubby4J but that
code uses different ports so it shouldn't clash with any manually run instance of Stubby4J