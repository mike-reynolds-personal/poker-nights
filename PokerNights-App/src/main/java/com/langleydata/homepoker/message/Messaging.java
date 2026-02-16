package com.langleydata.homepoker.message;

public interface Messaging {

	/** As an alternative to @SendToUser or to be used inside methods that use @SendTo
	 * sends to default queue of '/queue/private'
	 * @param sessionId The user session id
	 * @param payload The message payload
	 */
	void sendPrivateMessage(String sessionId, Object payload);

	/** As an alternative to @SendToUser or to be used inside methods that use @SendTo
	 * 
	 * @param sessionId The user session id
	 * @param queue The private queue to send to 
	 * @param payload The message payload
	 * @param delay Delay before sending message, in milliseconds
	 */
	void sendPrivateMessage(String sessionId, String queue, Object payload, long delay);

	/** Send a message to a specific game table
	 * 
	 * @param gameId The game's id
	 * @param message The message payload
	 */
	void sendBroadcastToTable(String gameId, Object message);

	/** Send a message to a specific game table with a delay
	 * 
	 * @param gameId The game's id
	 * @param message The message payload
	 * @param delay the delay in milliseconds
	 */
	void sendBroadcastToTable(String gameId, Object message, long delay);

}