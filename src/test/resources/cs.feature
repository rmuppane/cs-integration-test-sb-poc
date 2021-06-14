Feature: Test case 'cs'
    Scenario: 1
    		When a request to check for 'test-dmn.EvaluatedDesignOption1B2' the generated documents
	      |state   										|uk   				|
	      |zone												|London				|
	      |dateOfIncorporationMonths	|6						|
	      |dateOfIncorporation				|2021-01-01		|
	      |companyTypeEnName					|Ltd					|
	      |countryCode								|165					|
	      Then List of documents are
	      |  docName  | documentENname |  documentDEName  | documentITName  | documentFRName  |  documentInSourceAndCSLangRequired |  footNoteCodes  |
	      		|  UK-Doc-1a  | en-Document-1a | de-Document-1a | it-Document-1a | fr-Document-1a | true 														 	 |  3,5					 |
	      		|  UK-Doc-2a  | en-Document-2a | de-Document-2a | it-Document-2a | fr-Document-2a | true 														   |  3,7					 |