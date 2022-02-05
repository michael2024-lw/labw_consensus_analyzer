# consensus-analyzer

This repo contains code for analyzation consensus protocols represented in CNF form

## Basic instructions

Download a copy of PRISM and build it

* ``git clone https://github.com/prismmodelchecker/prism prism``
* ``cd prism/prism``
* ``make``

Download the ``consensus-analyzer`` repo and build the examples

* ``cd ../..``
* ``git clone https://github.com/prismmodelchecker/prism-api``
* ``cd consensus-analyzer``
* ``make``

## Further instructions

The second part of the above assumes that PRISM is in a directory called ``prism`` one level up.
If you want to use a PRISM distribution located elsewhere, build like this:

* ``make PRISM_DIR=/some/copy/of/prism``

Run ``bin/run``with parameters: 
* path to configuration file
* flag true if running with backward transitions, false otherwise

Run example: bin/run resources/cnf_simple.yaml true

In the output one can see the probability and expected number of messages for different combinations of backward transitions.

TODO:
* split configuration file on distinct 2 files: for probability and for specification
* figure out how to connect prism with maven and build the project as a single jar with fat jar
* for each backward transition create a separate .dot representation
* add formula conversion to CNF if it's in another form
* add negations to literals