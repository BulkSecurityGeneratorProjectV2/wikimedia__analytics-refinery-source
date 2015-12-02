/**
 * Copyright (C) 2014 Wikimedia Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wikimedia.analytics.refinery.core;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contains functions to find geo information of an IP address using Maxmind's GeoIP2
 *
 * TODO: Allow usage of this class without always instantiating both city and country databases.
 */
public class Geocode {
    // Default paths to Maxmind databases
    public static final String DEFAULT_DATABASE_COUNTRY_PATH  = "/usr/share/GeoIP/GeoIP2-Country.mmdb";
    public static final String DEFAULT_DATABASE_CITY_PATH     = "/usr/share/GeoIP/GeoIP2-City.mmdb";

    static final Logger LOG = Logger.getLogger(Geocode.class.getName());

    //Constants to hold the keys to use in geo-coded data map
    private static final String CONTINENT = "continent";
    private static final String COUNTRY_CODE = "country_code";
    private static final String COUNTRY = "country";
    private static final String SUBDIVISION = "subdivision";
    private static final String CITY = "city";
    private static final String POSTAL_CODE = "postal_code";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String TIMEZONE = "timezone";

    private static final String UNKNOWN_COUNTRY_CODE = "--";
    private static final String UNKNOWN_VALUE = "Unknown";

    private DatabaseReader countryDatabaseReader;
    private DatabaseReader cityDatabaseReader;


    /**
     * Constructs a Geocode object with the default Maxmind 2 database paths.
     * You can override either of the default database paths by setting
     * the 'maxmind.database.country' and/or 'maxmind.database.city' properties.
     */
    public Geocode() throws IOException {
        this(null, null);
    }

    /**
     * Constructs a Geocode object with the provided Maxmind 2 database paths.
     * These are 'optional', in that you may set either one to null.  If null,
     * the system properties 'maxmind.database.country' and 'maxmind.database.city'
     * will be checked for paths.  If these are not set, then this will default to
     * DEFAULT_DATABASE_PATH_COUNTRY and DEFAULT_DATABASE_PATH_CITY respectively.
     *
     * @param countryDatabasePath
     *      String path to Maxmind's country database
     * @param cityDatabasePath
     *      String path to Maxmind's city database
     */
    public Geocode(String countryDatabasePath, String cityDatabasePath) throws IOException {
        // Override database paths with System properties, if they exist
        if (countryDatabasePath == null) {
            countryDatabasePath = System.getProperty("maxmind.database.country", DEFAULT_DATABASE_COUNTRY_PATH);
        }
        if (cityDatabasePath == null) {
            cityDatabasePath = System.getProperty("maxmind.database.city", DEFAULT_DATABASE_CITY_PATH);
        }

        LOG.info("Geocode using Maxmind country database: " + countryDatabasePath);
        LOG.info("Geocode using Maxmind city database: "    + cityDatabasePath);

        countryDatabaseReader = new DatabaseReader.Builder(new File(countryDatabasePath)).build();
        cityDatabaseReader    = new DatabaseReader.Builder(new File(cityDatabasePath)).build();
    }

    /**
     * Gets the country code for the given IP
     * @param ip
     *      String IP address
     * @return
     *      String
     */
    public final String getCountryCode(final String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CountryResponse response = countryDatabaseReader.country(ipAddress);
            Country country = response.getCountry();
            String ret = country.getIsoCode();
            if (ret == null) {
                ret = UNKNOWN_COUNTRY_CODE;
            }
            return ret;
        } catch (UnknownHostException hEx) {
            LOG.warn(hEx);
            return UNKNOWN_COUNTRY_CODE;
        } catch (IOException iEx) {
            LOG.warn(iEx);
            return UNKNOWN_COUNTRY_CODE;
        } catch (GeoIp2Exception gEx) {
            LOG.warn(gEx);
            return UNKNOWN_COUNTRY_CODE;
        }
    }

    /**
     * Gets a map with geo-code fields for the given IP
     * @param ip
     *      String Ip address
     * @return
     *      Map
     */
    public final Map<String, Object> getGeocodedData(final String ip) {

        InetAddress ipAddress = null;
        //Initialize map with default values
        Map<String, Object> geoData = getDefaultMap();

        try {
            ipAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException hEx) {
            LOG.warn(hEx);
            return geoData;
        }

        CityResponse response = null;
        try {
            response = cityDatabaseReader.city(ipAddress);
        } catch (IOException iEx) {
            LOG.warn(iEx);
            return geoData;
        } catch (GeoIp2Exception gEx) {
            LOG.warn(gEx);
            return geoData;
        }

        if (response == null)
            return geoData;

        Continent continent = response.getContinent();
        if (continent != null) {
            String name = continent.getName();
            if (name != null) {
                geoData.put(CONTINENT, name);
            }
        }

        Country country = response.getCountry();
        if (country != null) {
            String name = country.getName();
            String isoCode = country.getIsoCode();
            if (name != null && isoCode != null) {
                geoData.put(COUNTRY, name);
                geoData.put(COUNTRY_CODE, isoCode);
            }
        }

        List<Subdivision> subdivisions = response.getSubdivisions();
        if (subdivisions != null && subdivisions.size() > 0) {
            Subdivision subdivision = subdivisions.get(0);
            if (subdivision != null) {
                String name = subdivision.getName();
                if (name != null) {
                    geoData.put(SUBDIVISION, name);
                }
            }
        }

        City city = response.getCity();
        if (city != null) {
            String name = city.getName();
            if (name != null) {
                geoData.put(CITY, name);
            }
        }

        Postal postal = response.getPostal();
        if (postal != null) {
            String code = postal.getCode();
            if (code != null) {
                geoData.put(POSTAL_CODE, code);
            }
        }

        Location location = response.getLocation();
        if (location != null) {
            Double lat = location.getLatitude();
            Double lon = location.getLongitude();
            if (lat != null && lon != null) {
                geoData.put(LATITUDE, lat);
                geoData.put(LONGITUDE, lon);
            }
            if (location.getTimeZone() != null)
                geoData.put(TIMEZONE, location.getTimeZone());
        }

        return geoData;
    }

    /**
     * Creates a new geo data map with default values for all fields
     * @return Map
     */
    private Map<String, Object> getDefaultMap() {
        Map<String, Object> defaultGeoData = new HashMap<String, Object>();
        defaultGeoData.put(CONTINENT, UNKNOWN_VALUE);
        defaultGeoData.put(COUNTRY_CODE, UNKNOWN_COUNTRY_CODE);
        defaultGeoData.put(COUNTRY, UNKNOWN_VALUE);
        defaultGeoData.put(SUBDIVISION, UNKNOWN_VALUE);
        defaultGeoData.put(CITY, UNKNOWN_VALUE);
        defaultGeoData.put(POSTAL_CODE, UNKNOWN_VALUE);
        defaultGeoData.put(LATITUDE, -1);
        defaultGeoData.put(LONGITUDE, -1);
        defaultGeoData.put(TIMEZONE, UNKNOWN_VALUE);

        return defaultGeoData;
    }

    /**
     * Translate a country code into the country name
     *
     * @param countryCode
     *
     * @return String country name
     */
    public static String getCountryName(String countryCode) {
            if (countryCode == null){
                countryCode = "";
            }
            Locale l = new Locale("", countryCode);
            String displayCountry = l.getDisplayCountry();
            return displayCountry.equals(countryCode) ? UNKNOWN_VALUE : displayCountry;
    }
}
