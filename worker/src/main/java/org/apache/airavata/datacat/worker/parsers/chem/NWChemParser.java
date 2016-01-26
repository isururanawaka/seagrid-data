/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
package org.apache.airavata.datacat.worker.parsers.chem;

import org.apache.airavata.datacat.worker.parsers.AbstractParser;
import org.apache.airavata.datacat.worker.parsers.ParserException;
import org.apache.airavata.datacat.worker.util.WorkerConstants;
import org.apache.airavata.datacat.worker.util.WorkerProperties;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.UUID;

public class NWChemParser extends AbstractParser {

    private final static Logger logger = LoggerFactory.getLogger(NWChemParser.class);

    public static final String NWCHEM_SCRIPT_FILE = "../parser-scripts/chem/nwchem.py";
    public static final String DEFAULT_NWCHEM_SCRIPT_FILE = "parser-scripts/chem/nwchem.py";
    private final String nwchemOutputFileName = "nwchem-output.json";
    private final String nwchemMoleculeImageFileName = "nwchem-molecule.png";

    private final String scriptFilePath;

    public NWChemParser() throws IOException {
        super();
        if (new File(NWCHEM_SCRIPT_FILE).exists()) {
            logger.info("Using configured nwchem parser (nwchem.py) file");
            scriptFilePath = NWCHEM_SCRIPT_FILE;
        } else {
            logger.info("Using default nwchem parser (nwchem.py) file");
            scriptFilePath = ClassLoader.getSystemResource(DEFAULT_NWCHEM_SCRIPT_FILE).getPath();

        }
    }

    @Override
    public JSONObject parse(String localFilePath, Map<String, Object> inputMetadata) throws Exception {
        String workingDir = WorkerProperties.getInstance().getProperty(WorkerConstants.WORKING_DIR, "/tmp");
        try{
            if(!workingDir.endsWith(File.separator)){
                workingDir += File.separator;
            }
            Process proc = Runtime.getRuntime().exec("python " + scriptFilePath + " " + localFilePath + " "
                    + workingDir + nwchemOutputFileName + " " + workingDir + nwchemMoleculeImageFileName);
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));
            String s;
            // read any errors from the attempted command
            String error = "";
            while ((s = stdError.readLine()) != null) {
                error += s;
            }
            if(error == null || !error.isEmpty()){
                logger.warn(error);
            }

            File outputFile = new File(workingDir + nwchemOutputFileName);
            if(outputFile.exists()){
                JSONParser jsonParser = new JSONParser();
                Object obj = jsonParser.parse(new FileReader(workingDir + nwchemOutputFileName));
                JSONObject jsonObject = (JSONObject) obj;

                //TODO populate other fields
                if(inputMetadata != null && inputMetadata.get("experimentId") != null) {
                    jsonObject.put("id", inputMetadata.get("experimentId"));
                    jsonObject.put("experimentId", inputMetadata.get("experimentId"));
                }else{
                    jsonObject.put("id", UUID.randomUUID().toString());
                }

//                try{
//                    byte[] imageBytes = Files.readAllBytes(Paths.get(workingDir + nwchemMoleculeImageFileName));
//                    BASE64Encoder encoder = new BASE64Encoder();
//                    jsonObject.put("MolecularImage", encoder.encode(imageBytes));
//                }catch(Exception ex){
//                    logger.error("Unable to read bytes from image file", ex);
//                }
                return jsonObject;
            }
            throw new Exception("Could not parse data");
        }catch (Exception ex){
            logger.error(ex.getMessage(), ex);
            throw new ParserException(ex);
        }finally {
            File outputFile = new File(workingDir+nwchemOutputFileName);
            if(outputFile.exists()){
                outputFile.delete();
            }
            outputFile = new File(workingDir+nwchemMoleculeImageFileName);
            if(outputFile.exists()){
                outputFile.delete();
            }
        }
    }
}