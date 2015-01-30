// Copyright 2014 Wikimedia Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.wikimedia.analytics.refinery.core;

import java.util.regex.Pattern;

/**
 * Static functions to work withh Wikimedia webrequest data.
 */
public class Webrequest {

    /**
     * Wikimedia-specific crawlers
     */
    private static final Pattern crawlerPattern = Pattern.compile(
        "(goo wikipedia|MediaWikiCrawler-Google|wikiwix-bot).*"
    );

    /**
     * Pattern for automatically-added subdomains that indicate zero,
     * or some similar portal-based interface to MW.
     */
    private static final Pattern uriHostPattern = Pattern.compile(
        "\\.(m|zero)\\."
    );

    /**
     * Consistent fragment of the user agent used by the Wikimedia
     * official mobile apps: used to identify app requests in
     * getAccessMethod.
     */
    private static final Pattern appAgentPattern = Pattern.compile(
        "WikipediaApp"
    );

    /**
     * Identify Wikimedia-specific crawlers; returns TRUE
     * if the user agent matches a known crawler.
     * @param    userAgent    the user agent associated with the request.
     * @return   boolean
     */
    public static boolean isCrawler(String userAgent) {
        return crawlerPattern.matcher(userAgent).find();
    }

    /**
     * Given an x_analytics field and the name of a key, return the
     * value associated with said key, or an empty string if the key
     * is not found.
     *
     * @param xAnalytics the x_analytics field entry.
     * @param key the key to search for the value of.
     * @return String
     */
    public static String getXAnalyticsValue(String xAnalytics, String key) {

        String value = "";

        int keyIndex = xAnalytics.indexOf(key);
        if(keyIndex == -1){
            return value;
        }

        int delimiterIndex = xAnalytics.indexOf(";", keyIndex);
        if(delimiterIndex == -1){
            value = xAnalytics.substring(keyIndex + key.length() + 1);
        } else {
            value = xAnalytics.substring(keyIndex + key.length() + 1, delimiterIndex);
        }

        //Done
        return value;
    }
    /**
     * Determines the method used for accessing the site - mobile web,
     * desktop, or app. If the user agent is an app agent, it's
     * mobile app; if the user agent is not, but it is to m. or
     * zero. domains, mobile web; otherwise, desktop.
     *
     * @param uriHost the value in the uri_host field.
     *
     * @param userAgent the user_agent.
     *
     * @return String
     */
    public static String getAccessMethod(String uriHost, String userAgent) {
        String accessMethod = "";

        if(appAgentPattern.matcher(userAgent).find()){
            accessMethod = "mobile app";
        } else if(uriHostPattern.matcher(uriHost).find()){
            accessMethod = "mobile web";
        } else {
            accessMethod = "desktop";
        }

        return accessMethod;
    }
}