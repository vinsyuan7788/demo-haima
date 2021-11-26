package com.demo.haima.client;

import com.demo.haima.client.connection.state.ClientConnectionState;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public abstract class Client {

    /******************************* State machine *******************************/
    /**
     * This method is used to get the client connection state
     *
     * @return
     */
    public abstract ClientConnectionState getClientConnectionState();

    /******************************* Getter and Setter *******************************/

    /**
     * This method is used to get the session ID
     *
     * @return
     */
    public abstract long getSessionId();

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to close this client object.
     * Once the client is closed, its session becomes invalid.
     *
     * @throws InterruptedException
     */
    public abstract void close();

    /******************************* Business Method *******************************/

    /**
     * Get a Snowflake Id for the given app code. The Id will be unit according the whole.
     * <p>
     * Snowflake is the only solution for the primary key of data warehouse in the cloud,
     * for all your data & all your users. Conventional data warehouses and big data solutions
     * need an effective solution to the primary key.
     * <p>
     * If a primary key with the same actual value already exists in the DB Server, a
     * DBException with error code primary key conflict will be thrown. Note that since
     * we have very complicated business logic, so we re-construct the structure and
     * the algorithm within the Haima Server and provide the simple usage for you
     * to get the snowflake Id by the Haima Client API.
     * <p>
     * If you WANT to get deep acknowledge of our specific SnowFlake structure, we have
     * detail description of the structure of the SnowFlake Id. Also, we have put it
     * into the wiki for you to get understood.
     * @Link wiki http://...........................
     * <p>
     * The maximum size of the SnowFlake Id is 19 digits (to the year 2034).
     * Sequence larger than this will cause a DBException when used it.
     *
     * @param app
     *                the app code for the resulting SnowFlake Id
     * @return the SnowFlake id of the given app code
     * @throws Exception
     */
    public abstract long getSnowFlakeId(final int app) throws Exception;
}
