// w
// translator.java 
// -------------------------------------
// part of YACY
// (C) by Michael Peter Christen; mc@anomic.de
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// This file ist contributed by Alexander Schier
// last major change: 25.05.2005
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Using this software in any meaning (reading, learning, copying, compiling,
// running) means that you agree that the Author(s) is (are) not responsible
// for cost, loss of data or any harm that may be caused directly or indirectly
// by usage of this softare or this documentation. The usage of this software
// is on your own risk. The installation and usage (starting/running) of this
// software may allow other people or application to access your computer and
// any attached devices and is highly dependent on the configuration of the
// software which must be done by the user of the software; the author(s) is
// (are) also not responsible for proper configuration and usage of the
// software, even if provoked by documentation provided together with
// the software.
//
// Any changes to this file according to the GPL as documented in the file
// gpl.txt aside this file in the shipment you received can be done to the
// lines that follows this copyright notice here, but changes must not be
// done inside the copyright notive above. A re-distribution must contain
// the intact and unchanged copyright notice.
// Contributions and changes to the program code must be marked as such.

package de.anomic.data;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.lang.IndexOutOfBoundsException;

import de.anomic.server.logging.serverLog;
import de.anomic.data.listManager;

/**
 * Wordlist based translator
 *
 * Uses a Property like file with phrases or single words to translate a string or a file
 * */
public class translator {
	public static String translate(String source, Hashtable translationList){
		Enumeration keys = translationList.keys();
		String result = source;
		String key = "";
		while(keys.hasMoreElements()){
			key = (String)keys.nextElement();
			result = result.replaceAll(key, (String)translationList.get(key));
			//System.out.println("Replaced \""+key+"\" by \""+translationList.getProperty(key)+"\""); //DEBUG
		}
		return result;
	}
	
	public static Hashtable loadTranslationsLists(File translationFile){
		Hashtable lists = new Hashtable(); //list of translationLists for different files.
		Hashtable translationList = new Hashtable(); //current Translation Table
        
		ArrayList list = listManager.getListArray(translationFile);
		Iterator it = list.iterator();
		String line = "";
		String value = "";
		String[] splitted;
		String forFile="";

		while(it.hasNext()){
			line = (String)it.next();
			if(! line.startsWith("#")){
				splitted = line.split("==");
				if(splitted.length == 2){
					translationList.put(splitted[0], splitted[1]);
				}else if(splitted.length > 2){
					value = "";
					for(int i = splitted.length-1;i>=1;i--){
						value += splitted[i];
					}
					translationList.put(splitted[0], value);
				}else{ //Invalid line
				}
			}else if(line.startsWith("#File: ")){
				if(forFile != ""){
						lists.put(forFile, translationList);
				}
				if(line.charAt(6)==' '){
					forFile=line.substring(7);
				}else{
					forFile=line.substring(6);
				}
				translationList=new Hashtable();
			}
		}
		lists.put(forFile, translationList);
		return lists;
	}
	public static boolean translateFile(File sourceFile, File destFile, File translationFile){
		Hashtable translationList = (Hashtable)loadTranslationsLists(translationFile).get(sourceFile.getName());
		return translateFile(sourceFile, destFile, translationList);
	}
	
	public static boolean translateFile(File sourceFile, File destFile, Hashtable translationList){

		String content = "";
		String line = "";
        BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
			while( (line = br.readLine()) != null){
				content += line + de.anomic.server.serverCore.crlfString;
			}
			br.close();
		}catch(IOException e){
			return false;
		} finally {
            if (br!=null)try{br.close();}catch(Exception e){}
        }
        
		content = translate(content, translationList);
        BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new FileWriter(destFile));
			bw.write(content);
			bw.close();
		}catch(IOException e){
			return false;
		} finally {
            if(bw!=null)try{bw.close();}catch(Exception e){}
        }
		
		return true;
	}

	public static boolean translateFiles(File sourceDir, File destDir, File baseDir, File translationFile, String extensions){
			Hashtable translationLists = loadTranslationsLists(translationFile);
			return translateFiles(sourceDir, destDir, baseDir, translationLists, extensions);
	}

	public static boolean translateFiles(File sourceDir, File destDir, File baseDir, Hashtable translationLists, String extensions){
		destDir.mkdirs();
		File[] sourceFiles = sourceDir.listFiles();
        Vector exts=listManager.string2vector(extensions);
        boolean rightExtension;
        Iterator it;
        String relativePath;
		for(int i=0;i<sourceFiles.length;i++){
             it=exts.iterator();
             rightExtension=false;
             while(it.hasNext()){
                 if(sourceFiles[i].getName().endsWith((String) it.next())){
                     rightExtension=true;
                     break;
                 }
             }
			if(rightExtension){
                try{
                    relativePath=sourceFiles[i].getAbsolutePath().substring(baseDir.getAbsolutePath().length()+1, sourceFiles[i].getAbsolutePath().length()); //+1 to get the "/"
                    relativePath = relativePath.replace(File.separatorChar, '/');
                }catch(IndexOutOfBoundsException e){
					serverLog.logSevere("Translator", "Error creating relative Path for "+sourceFiles[i].getAbsolutePath());
                    relativePath="wrong path"; //not in translationLists
                } 
				if(translationLists.containsKey(relativePath)){
					if( translateFile(sourceFiles[i], new File(destDir, sourceFiles[i].getName().replace('/', File.separatorChar)), (Hashtable)translationLists.get(relativePath))){
						serverLog.logInfo("Translator", "Translated file: "+ relativePath);
					}else{
						serverLog.logSevere("Translator", "File error while translating file "+relativePath);
					}
				}else{
						//serverLog.logInfo("Translator", "No translation for file: "+relativePath);
				}
			}

		}
		return true;
	}

    public static boolean translateFilesRecursive(File sourceDir, File destDir, File translationFile, String extensions, String notdir){
        ArrayList dirList=listManager.getDirsRecursive(sourceDir, notdir);
        dirList.add(sourceDir);
        Iterator it=dirList.iterator();
        File file=null;
        File file2=null;
        while(it.hasNext()){
            file=(File)it.next();
            //cuts the sourcePath and prepends the destPath
            file2=new File(destDir, file.getPath().substring(sourceDir.getPath().length(), file.getPath().length()));
            //file2=new File(file.getPath().replaceFirst(sourceName.replaceAll("\\\\", "\\\\"), destName));
            if(file.isDirectory() && !file.getName().equals(notdir)){
                //file2.mkdirs();
                translateFiles(file, file2, sourceDir, translationFile, extensions);
                //translateFilesRecursive(file, file2, translationFile, extension, notdir);
            }/*else{
                translateFiles(file, file2, translationFile, extension);
            }*/
        }
        return true;
    }

}
