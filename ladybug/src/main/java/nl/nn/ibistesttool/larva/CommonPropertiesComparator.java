/*
   Copyright 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.ibistesttool.larva;

import java.util.Comparator;

public class CommonPropertiesComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int rank1 = getRank(o1), rank2 = getRank(o2);
//        if (rank1 == -1 || rank2 == -1) return -1;
        int rankDiff = Integer.compare(rank1, rank2);
        if (rankDiff != 0) {
            return rankDiff;
        } else {
            return String.CASE_INSENSITIVE_ORDER.compare(o1, o2);
        }
    }

    private static int getRank(String s) {
        s = s.trim();
        String[] sParts = s.split("\\.");
        switch (sParts[0]) {
            case "include":
                return 0;
            case "adapter":
                return 1;
            case "stub":
                return 2;
            default:
                if (sParts[0].startsWith("ignoreContentBetweenKeys")) {
                    String ignoreIdx = sParts[0].substring(24);
                    if (ignoreIdx.isEmpty()) return 3;
                    return 3 + 2 * Integer.parseInt(ignoreIdx) + Integer.parseInt(sParts[sParts.length - 1].substring(3));
                }
        }
        return -1;
    }
}
