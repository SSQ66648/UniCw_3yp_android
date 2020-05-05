/*--------------------------------------
    CONSTANTS
--------------------------------------*/
    private static final String TAG = ${NAME}.class.getSimpleName();

    
    
    /*--------------------------------------
        MEMBER VARIABLES
    --------------------------------------*/
    //---LAYOUT---

    //---VARIABLES---


    /*--------------------------------------
        LIFECYCLE
    --------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.***???***);
        Log.d(TAG, "onCreate: ");
        
        //---TOOLBAR---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        
        //---VIEWS----

        //---EXECUTE---    
    }
    
    

    /*--------------------------------------
        LISTENERS
    --------------------------------------*/


    /*--------------------------------------
        METHODS
    --------------------------------------*/
