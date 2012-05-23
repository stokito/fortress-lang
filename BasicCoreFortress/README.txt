Example bcf code to run through interpreter:
Boolean.bcf
Arith.bcf

To run unit tests:		scala CFTest test

To play with example files: 	
		scala CFTest
		BCF> :load Boolean.bcf
		BCF> :load Arith.bcf
		BCF> Zero().inc().equals(Succ(Zero()))
