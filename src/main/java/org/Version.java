/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

*/

package org;

import quicktime.std.comp.ComponentDescription;

/**
 * Version class provides version and copyright strings for SmartFrog System.
 */
public class Version {


    // Dont' change this. MODIFY version.sf in same package!!!!!!!!!!!!!!!!!!!
    private static String name=        "SmartFrog";
    private static String majorRelease="3";
    private static String minorRelease="4";
    private static String build=       "17"; // odd numbers are development versions
    private static String status=      ""; //alpha, beta, stable

    private static String minCoreVersion = null;

    private static String maxCoreVersion = null;

    // Dont' change this. MODIFY version.sf in same package!!!!!!!!!!!!!!!!!!!
    /** The copyright String for the SmartFrog system. */
    private static String copyright = "(C) Copyright 1998-2006 HP Development Company, LP";
 

    /**
     *
     * @return String Complete Version String
     */
    public static String versionString(){
        //init(); 
        String newStatus=null;
        if (!status.trim().equals("")){
            newStatus="_"+status;
        } else newStatus="";
        return name+" "+majorRelease+"."+minorRelease+"."+build+newStatus;
    }

    /**
     *
     * @return String Complete Version String
     */
    public static String versionStringforrelease(){
        //init(); 
        String newStatus=null;
        if (!status.trim().equals("")){
            newStatus="_"+status;
        } else newStatus="";
        return majorRelease+"."+minorRelease+"."+build+newStatus;
    }

    /**
     * Major release number.
     */
    public static String majorRelease(){ 
        return majorRelease;
    }

    /**
     *Minor release number.
     */
    public static String minorRelease(){ 
        return minorRelease;
    }
    /**
     * Build number.
     */
    public static String build(){ 
        return build;
    }

    /**
     * Status [alpha, beta, (Empty when statable)].
     */
    public static String status(){ 
        return status;
    }

    public static String copyright(){
        //init(); 
        return copyright;
    }

    /**
     * Min Core compatible version
     * If null, it is considred compatible with all
     */
    public static String minCoreVersion(){ 
        return minCoreVersion;
    }

    /**
     * Max Core compatible version
     * If null, it is considered compatible with all version numbers bigger
     * than ours
     */
    public static String maxCoreVersion(){ 
        return maxCoreVersion;
    }


    /**
     * Checks is version provides is compatible with this version.
     * @param version String Version has to be of the form: MajorRelease.MinorRelease.Build_status.
     * @return boolean
     */
    public static boolean compatible(String version){ 
        String majorRelease = version.substring(0,version.indexOf('.'));
        String cutVersion = version.substring(majorRelease.indexOf('.'));
        String minorRelease = cutVersion.substring(0,cutVersion.indexOf('.'));
        cutVersion = version.substring(majorRelease.indexOf('.'));
        String build = cutVersion.substring(0,cutVersion.indexOf('_'));

        boolean compatible = true;
        compatible = checkMaxVersion(majorRelease, minorRelease, build);
        compatible = checkMinVersion(majorRelease, minorRelease, build);
        return compatible;
    }


    //@todo review this matching method.
    private static boolean checkMaxVersion (String majorReleaseN, String minorReleaseN, String buildN){
        if (majorRelease==null) return true;
        if (!(Integer.parseInt(majorRelease) < (Integer.parseInt(majorReleaseN)))){return false;}
        if (!(Integer.parseInt(majorRelease) == (Integer.parseInt(majorReleaseN)))){
            if (!(Integer.parseInt(minorRelease) < (Integer.parseInt(minorReleaseN)))){return false;}
            if (!(Integer.parseInt(minorRelease) == (Integer.parseInt(minorReleaseN)))){
                if (!(Integer.parseInt(build) < (Integer.parseInt(buildN)))){return false;}
                if (!(Integer.parseInt(build) == (Integer.parseInt(buildN)))){
                    return true; // All numbers are equal :-)
                }
            }
        }

        return true;
    }


    //@todo review this matching method.
    private static boolean checkMinVersion (String majorReleaseN, String minorReleaseN, String buildN){
        if (majorRelease==null) return true;
        if (!(Integer.parseInt(majorRelease) > (Integer.parseInt(majorReleaseN)))){return false;}
        if (!(Integer.parseInt(majorRelease) == (Integer.parseInt(majorReleaseN)))){
            if (!(Integer.parseInt(minorRelease) > (Integer.parseInt(minorReleaseN)))){return false;}
            if (!(Integer.parseInt(minorRelease) == (Integer.parseInt(minorReleaseN)))){
                if (!(Integer.parseInt(build) > (Integer.parseInt(buildN)))){return false;}
                if (!(Integer.parseInt(build) == (Integer.parseInt(buildN)))){
                    return true; // All numbers are equal :-)
                }
            }
        }
        return true;
    } 

    /**
     * Method invoked to print the version info.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
	if ((args.length > 0) && (args[0].equals("-b")))
	    System.out.print(versionStringforrelease());
	else if ((args.length > 0) && (args[0].equals("-min")))
	    System.out.print(minCoreVersion());
	else if ((args.length > 0) && (args[0].equals("-max")))
	    System.out.print(maxCoreVersion());
	else     	
	    System.out.print(versionString());
    }
}
