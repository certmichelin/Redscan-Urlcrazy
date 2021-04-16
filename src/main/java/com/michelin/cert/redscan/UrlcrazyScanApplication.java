/**
 * Michelin CERT 2020.
 */

package com.michelin.cert.redscan;

import com.michelin.cert.redscan.utils.system.OsCommandExecutor;
import com.michelin.cert.redscan.utils.system.StreamGobbler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONArray;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RedScan scanner main class.
 *
 * @author Maxime ESCOURBIAC
 * @author Sylvain VAISSIER
 * @author Maxence SCHMITT
 */
@SpringBootApplication
public class UrlcrazyScanApplication {


  @Autowired
  private DatalakeConfig datalakeConfig;

  /**
   * Constructor to init rabbit template. Only required if pushing data to queues
   *
   * @param rabbitTemplate Rabbit template.
   */
  public UrlcrazyScanApplication(RabbitTemplate rabbitTemplate) {
    
  }

  /**
   * RedScan Main methods.
   *
   * @param args Application arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(UrlcrazyScanApplication.class, args);
  }

  /**
   * Message executor.
   *
   * @param message Message received.
   */
  @RabbitListener(queues = {RabbitMqConfig.QUEUE_MASTERDOMAINS})
  public void receiveMessage(String message) {
    LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("Starting urlcrazy on : %s -- Layout: Qwerty", message));
    String qwTempOutFile = null;
    String azTempOutFile = null;
    JSONArray finalResult = new JSONArray();
    try {
      qwTempOutFile = String.format("%sqwerty.json", message);
      azTempOutFile = String.format("%sazerty.json", message);
      OsCommandExecutor osCommandExecutor = new OsCommandExecutor();
      StreamGobbler streamGobbler = osCommandExecutor.execute(String.format("urlcrazy/urlcrazy -k qwerty -f JSON -o %s %s", qwTempOutFile, message));
      if (streamGobbler != null) {
        LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("Subfinder terminated with status : %d", streamGobbler.getExitStatus()));
      }
      LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("Starting Urlcrazy on %s -- Layout: Azerty", message));
      streamGobbler = osCommandExecutor.execute(String.format("urlcrazy/urlcrazy -k azerty -f JSON -o %s %s", azTempOutFile, message));
      if (streamGobbler != null) {
        LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("Subfinder terminated with status : %d", streamGobbler.getExitStatus()));
      }
      finalResult = jsonResultFormatter(azTempOutFile, qwTempOutFile);//JsonResultFormatter(azTempOutFile, qwTempOutFile);
    } catch (Exception ex) {
      LogManager.getLogger(UrlcrazyScanApplication.class).error(String.format("Exception : %s", ex.getMessage()));
    } finally {
      if (qwTempOutFile != null) {
        File qwToDelete = new File(qwTempOutFile);
        if (qwToDelete.delete()) {
          LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("First temp file deleted"));
        }
      }
      if (azTempOutFile != null) {
        File azToDelete = new File(azTempOutFile);
        if (azToDelete.delete()) {
          LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("Second temp file deleted"));
        }
      }
    }
    
  
    if (!finalResult.isEmpty()) {
      datalakeConfig.upsertMasterDomainField(message, "urlcrazy", finalResult);
    }
  
 
  }
  /**
   * 
   * @param azTempOutFile Output Json file for azerty layout.
   * @param qwTempOutFile Output Json file for qwerty layout.
   * @return returningJson Formatted json to insert in elastic.
   */
  public JSONArray jsonResultFormatter(String azTempOutFile, String qwTempOutFile) {
    JSONParser parser = new JSONParser();
    JSONArray returningJson = new JSONArray();
    try {
      LogManager.getLogger(UrlcrazyScanApplication.class).info(String.format("JsonresultFormatter"));
      Object obj = parser.parse(new FileReader(azTempOutFile));
      JSONObject jsonObj = (JSONObject) obj;
      JSONArray jsonTypos = (JSONArray) jsonObj.get("typos");
      for (Object o : jsonTypos) {
        JSONObject jsonCursor = (JSONObject) o;
        if (!jsonCursor.get("resolved_a").toString().isEmpty() 
                && !jsonCursor.get("type").toString().contains("All SLD") 
                && !jsonCursor.get("type").toString().contains("Wrong TLD")) { 
          if (!returningJson.contains(jsonCursor)) {
            returningJson.add(jsonCursor);
          }
          
        }
      }
      Object qwobj = parser.parse(new FileReader(qwTempOutFile));
      JSONObject qwjsonObj = (JSONObject) qwobj;
      JSONArray qwjsonTypos = (JSONArray) qwjsonObj.get("typos");
      for (Object o : qwjsonTypos) {
        JSONObject jsonCursor = (JSONObject) o;
        if (!jsonCursor.get("resolved_a").toString().isEmpty()
                && !jsonCursor.get("type").toString().contains("All SLD")
                && !jsonCursor.get("type").toString().contains("Wrong TLD")) {  
          if (!returningJson.contains(jsonCursor)) {
            returningJson.add(jsonCursor);
          }

        }    
      }
    } catch (FileNotFoundException e) {
      LogManager.getLogger(UrlcrazyScanApplication.class).error(String.format("Error with json file: not found : %s", e.toString()));
    } catch (IOException e) {
      LogManager.getLogger(UrlcrazyScanApplication.class).error(String.format("Error with json file: IO : %S", e.toString()));
    } catch (ParseException e) {
      LogManager.getLogger(UrlcrazyScanApplication.class).error(String.format("Error with json file: Parsing %s", e.toString()));
    }
    return returningJson;
  }
}


  
