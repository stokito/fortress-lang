/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import com.sun.fortress.nodes.APIName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JavaDBRepository extends StubRepository implements FortressRepository {

    /* 
     * The Derby embedded driver. 
     */ String driver = "org.apache.derby.jdbc.EmbeddedDriver";

    /*
     * The database name. 
     */ String dbName = "FortressRepository";

    /*
     * The Derby prefix for database URLs.
     */ String urlPrefix = "jdbc:derby:";

    /*
     * The connection URL to use.
     */ String connectionURL = urlPrefix + dbName + ";create=true";

    /* 
     * The disconnection URL to use.
     */ String disconnectionURL = urlPrefix + ";shutdown=true";

    /*
     * Our handle on the connection to the resident fortress 
     */ Connection conn;

    /*
     * Connect to the resident fortress (creating it if it doesn't already exist).
     */
    public void connect() {
        try {
            // This call to Class.forName is not necessary on Java 6. We include it for robustness.
            Class.forName(driver);

            conn = DriverManager.getConnection(connectionURL);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (SQLException e) {
            printSQLException(e);
            System.err.println("FAIL: Connection to the resident fortress failed!");
        }
    }

    public void disconnect() {
        try {
            conn.close();
            DriverManager.getConnection(disconnectionURL);
        }
        catch (SQLException e) {
            // Derby signals shutdown by throwing an exception!
            // Check SQLState to ensure normal shutdown.
            if (!e.getSQLState().equals("XJ015")) {
                printSQLException(e);
                System.err.println("FAIL: The resident fortress did not shut down normally!");
            }
        }
    }

    private void printSQLException(SQLException e) {
        while (e != null) {
            System.out.println("\n---SQLException---\n");
            System.out.println("SQLState:   " + e.getSQLState());
            System.out.println("Severity: " + e.getErrorCode());
            System.out.println("Message:  " + e.getMessage());
            e.printStackTrace();

            e = e.getNextException();
        }
    }


    /**
     * Provide an updating view of the apis present in the repository.
     * Need not support mutation.
     */
    //    public Map<APIName, ApiIndex> apis() { 

    //              //  Prepare the insert statement to use

    //             psInsert = conn.prepareStatement("insert into WISH_LIST(WISH_ITEM) values (?)");


    //             //   ## ADD / DISPLAY RECORD SECTION ##

    //             //  The Add-Record Loop - continues until 'exit' is entered

    //             do {

    //                 // Call utility method to ask user for input

    //                 answer = WwdUtils.getWishItem();

    //                 // Check if it is time to EXIT, if not insert the data

    //                 if (! answer.equals("exit"))

    //                 {

    //                     //Insert the text entered into the WISH_ITEM table

    //                     psInsert.setString(1,answer);

    //                     psInsert.executeUpdate();


    //                     //   Select all records in the WISH_LIST table

    //                     myWishes = s.executeQuery("select ENTRY_DATE, WISH_ITEM from WISH_LIST order by ENTRY_DATE");


    //                     //  Loop through the ResultSet and print the data

    //                     System.out.println(printLine);

    //                     while (myWishes.next())

    //                      {

    //                            System.out.println("On " + myWishes.getTimestamp(1) + " I wished for " + myWishes.getString(2));

    //                       }

    //                       System.out.println(printLine);

    //                       //  Close the resultSet

    //                       myWishes.close();

    //                  }       //  END of IF block

    //              // Check if it is time to EXIT, if so end the loop

    //               } while (! answer.equals("exit")) ;  // End of do-while loop


    //              // Release the resources (clean up )

    //              psInsert.close();

    //              s.close();

    //             conn.close();

    //             System.out.println("Closed connection");

    //    }

    /**
     * Provide an updating view of the components present in the repository.
     * Need not support mutation.
     */
    //public Map<APIName, ComponentIndex> components();

    /**
     * Add a compiled/processed api to the repository.
     */
    //public void addApi(APIName name, ApiIndex definition);

    /**
     * Add a compiled/processed component to the repository.
     */
    //public void addComponent(APIName name, ComponentIndex definition);

    /**
     * Removes the AST for the component form any in-memory caches and/or maps,
     * and optionally remove it from any stable storage as well.
     *
     * Used to avoid memory leaks in unit testing, and to clear non-standard
     * scribbles from the cache.
     *
     * @param name
     * @param andTheFileToo
     */
    //public void deleteComponent(APIName name, boolean andTheFileToo);

    /**
     * Retrieve an api from the repository given a name.
     */
    //public ApiIndex getApi(APIName name) throws FileNotFoundException, IOException;

    /**
     * Retrieve a component from the repository given a name.
     */
    //public ComponentIndex getComponent(APIName name) throws FileNotFoundException, IOException, StaticError;

    /**
     * Retrieve a component from the repository that is linked properly to other components.
     */
    //public ComponentIndex getLinkedComponent(APIName name) throws FileNotFoundException, IOException, StaticError;

    /**
     * Return the last modification date of an api.
     */
    //public long getModifiedDateForApi(APIName name) throws FileNotFoundException ;

    /**
     * Return the last modification date of a component.
     */
    //public long getModifiedDateForComponent(APIName name) throws FileNotFoundException ;

    /**
     * True if this API has a foreign implementation.
     *
     * @param name
     * @return
     */
    //public boolean isForeign(APIName name);

    /**
     * Debugging methods.
     */
    //public boolean setVerbose(boolean new_value);
    //public boolean verbose();

    /**
     * Clear
     */

    //public void clear();
    public boolean isForeign(APIName name) {
        return false;
    }
}


