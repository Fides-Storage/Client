package org.fides.client.tools;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.fides.components.Actions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * An util that can be used to send action requests to an OutputStream.
 */
public final class CommunicationUtil {

	private CommunicationUtil() {
	}

	/**
	 * Copies an action request to the OutputStream
	 * 
	 * @param outputStream
	 *            The stream to use
	 * @param action
	 *            The action to copy to the stream
	 * @throws IOException
	 */
	public static void requestAction(DataOutputStream outputStream, String action) throws IOException {
		JsonObject actionRequest = new JsonObject();
		actionRequest.addProperty(Actions.ACTION, action);
		outputStream.writeUTF(new Gson().toJson(actionRequest));
	}

	/**
	 * Copies an action request with properties to the OutputStream
	 * 
	 * @param outputStream
	 *            The stream to use
	 * @param action
	 *            The action to copy to the stream
	 * @param properties
	 *            The properties associated with the action request
	 * @throws IOException
	 */
	public static void requestActionWithProperties(DataOutputStream outputStream, String action, Map<String, Object> properties) throws IOException {
		JsonObject returnJsonObject = new JsonObject();
		returnJsonObject.addProperty(Actions.ACTION, action);
		for (Map.Entry<String, Object> property : properties.entrySet()) {
			Object value = property.getValue();
			if (value instanceof String) {
				returnJsonObject.addProperty(property.getKey(), (String) value);
			} else if (value instanceof Number) {
				returnJsonObject.addProperty(property.getKey(), (Number) value);
			} else if (value instanceof Boolean) {
				returnJsonObject.addProperty(property.getKey(), (Boolean) value);
			} else if (value instanceof Character) {
				returnJsonObject.addProperty(property.getKey(), (Character) value);
			} else {
				throw new IllegalArgumentException("Object may only be of type: String, Number, Boolean or Character");
			}
		}
		outputStream.writeUTF(new Gson().toJson(returnJsonObject));
	}

}
