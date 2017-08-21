/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.slicedpie;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import com.fanniemae.ezpie.JobManager;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-08-17
 * 
 */

@Path("/v1")
public class V1_ezPie {
	
	@Path("/status")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response data() {
		String json="[{\"Message\": \"SlicedPie responding.\"}]";
		return Response.ok(json).build();
	}

	@Path("/data")
	@POST
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON })
	@Produces(MediaType.APPLICATION_JSON)
	public Response datapost(String jsonMessage) {
		if (jsonMessage == null) {
			return Response.status(400).entity("JSON message body is null.").build();			
		}
		//System.out.println(jsonMessage);
		try {
			JSONObject jobject = new JSONObject(jsonMessage);
			String job = jobject.optString("job");
			if ((job == null) || job.isEmpty()) {
				throw new IllegalArgumentException("Message body is missing a value for job.");
			}
			
			String settingsFile = getSettingsFile(); // "C:\\Developers\\Code\\TestDirectory\\_Settings.xml";
			JobManager jm = new JobManager( settingsFile, job, null);

			Map<String, String> tokens = new HashMap<String,String>();
			Iterator<String> keys = jobject.keys();
			while(keys.hasNext()) {
				String key = (String) keys.next();
				tokens.put(key, jobject.optString(key));
			}
			jm.addTokens(tokens);

			String data = jm.getDataJson();
			//System.out.println(data);
			return Response.ok(data).build();			
		} catch (JSONException ex) {
			JSONObject error = new JSONObject();
			error.put("Error", String.format("Message body is not valid JSON. %s", ex.getMessage()));
			return Response.status(400).entity(error.toString()).build();			
		} catch (Exception ex) {
			JSONObject error = new JSONObject();
			error.put("Error", ex.getMessage());
			return Response.status(500).entity(error.toString()).build();			
		}
	}	
	
	@Context ServletContext context;
	public String getSettingsFile() {
		return context.getInitParameter("SettingsFile");
	}
	
}