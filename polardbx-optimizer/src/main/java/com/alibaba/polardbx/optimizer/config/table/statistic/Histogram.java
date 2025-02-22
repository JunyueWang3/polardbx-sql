/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.optimizer.config.table.statistic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Histogram {

    private static final Logger logger = LoggerFactory.getLogger("statistics");

    private List<Bucket> buckets = new ArrayList<>();

    private DataType dataType;

    private int maxBucketSize;

    private float sampleRate;

    public Histogram(int maxBucketSize, DataType dataType, float sampleRate) {
        this.maxBucketSize = maxBucketSize;
        this.dataType = dataType;
        if (sampleRate < 0f || sampleRate > 1f) {
            sampleRate = 1f;
        }
        this.sampleRate = sampleRate;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void buildFromData(Object[] data) {
        if (data == null || data.length == 0) {
            return;
        }

        Arrays.sort(data, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return dataType.compare(o1, o2);
            }
        });

        int numPerBucket = (int) Math.ceil((float) data.length / maxBucketSize);
        buckets.add(newBucket(data[0], 0));
        for (int i = 1; i < data.length; i++) {
            Bucket bucket = buckets.get(buckets.size() - 1);
            Object upper = bucket.upper;
            if (dataType.compare(upper, data[i]) == 0) {
                bucket.count++;
            } else if (bucket.count < numPerBucket) { // not full, assert dataType.compare(upper, data[i]) < 1
                bucket.count++;
                bucket.ndv++;
                bucket.upper = data[i];
            } else { // full
                buckets.add(newBucket(data[i], bucket.preSum + bucket.count));
            }
        }
    }

    private Bucket newBucket(Object value, int preSum) {
        Bucket bucket = new Bucket();
        bucket.lower = value;
        bucket.upper = value;
        bucket.count = 1;
        bucket.preSum = preSum;
        bucket.ndv = 1;
        return bucket;
    }

    /**
     * find the first bucket such that key<=bucket.upper
     *
     * @param key the key to find
     * @return the bucket found, null if not found
     */
    private Bucket findBucket(Object key) {
        if (buckets.isEmpty()) {
            return null;
        }
        int left = 0;
        int right = buckets.size() - 1;
        //invariant: key<=buckets[right].upper
        if (dataType.compare(key, buckets.get(right).upper) > 0) {
            return null;
        }
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (dataType.compare(key, buckets.get(mid).upper) > 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return buckets.get(right);
    }

    public long rangeCount(Object lower, boolean lowerInclusive, Object upper, boolean upperInclusive) {
        try {
            double count = rangeCountIgnoreSampleRate(lower, lowerInclusive, upper, upperInclusive);
            return (long) Math.max((count) / sampleRate, 0);
        } catch (Throwable e) {
            // dataType.compare may throw error
            logger.error(e);
            return 0;
        }
    }

    private int rangeCountIgnoreSampleRate(Object lower, boolean lowerInclusive, Object upper, boolean upperInclusive) {
        if (buckets.isEmpty()) {
            return 0;
        }
        if (lower != null && upper == null) {
            if (lowerInclusive) {
                return greatEqualCount(lower);
            } else {
                return greatCount(lower);
            }
        } else if (lower == null && upper != null) {
            if (upperInclusive) {
                return lessEqualCount(upper);
            } else {
                return lessCount(upper);
            }
        } else if (lower != null && upper != null) {
            int cmp = dataType.compare(lower, upper);
            if (cmp > 0) {
                return 0;
            } else if (cmp == 0) {
                if (!lowerInclusive || !upperInclusive) {
                    return 0;
                } else {
                    Bucket bucket = findBucket(upper);
                    if (bucket == null) {
                        return 0;
                    } else {
                        return bucket.count / bucket.ndv;
                    }
                }
            }
            Bucket lowerBucket = findBucket(lower);
            if (lowerBucket == null) {
                return 0;
            }
            Bucket upperBucket = findBucket(upper);
            if (upperBucket == null) {
                return greatEqualCount(lower);
            }
            if (lowerInclusive && upperInclusive) {
                return totalCount() - lessCount(lower) - greatCount(upper);
            } else if (lowerInclusive && !upperInclusive) {
                return totalCount() - lessCount(lower) - greatEqualCount(upper);
            } else if (!lowerInclusive && upperInclusive) {
                return totalCount() - lessEqualCount(lower) - greatCount(upper);
            } else {
                return totalCount() - lessEqualCount(lower) - greatEqualCount(upper);
            }
        } else {
            return totalCount();
        }
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    private int totalCount() {
        if (buckets.isEmpty()) {
            return 0;
        } else {
            Bucket lastBucket = buckets.get(buckets.size() - 1);
            return lastBucket.preSum + lastBucket.count;
        }
    }

    private int lessCount(Object u) {
        Bucket bucket = findBucket(u);
        if (bucket == null) {
            if (buckets.isEmpty()) {
                return 0;
            }
            if (dataType.compare(u, buckets.get(0).lower) < 0) {
                return 0;
            }
            if (dataType.compare(u, buckets.get(buckets.size() - 1).upper) > 0) {
                return totalCount();
            }
            logger.error("impossible case appear in lessCount");
            return totalCount();
        } else {
            if (dataType.compare(u, bucket.lower) == 0) {
                return bucket.preSum;
            } else if (dataType.compare(u, bucket.upper) == 0) {
                return bucket.preSum + bucket.count - bucket.count / bucket.ndv;
            } else {
                Double min = convertToDouble(bucket.lower, bucket);
                Double max = convertToDouble(bucket.upper, bucket);
                Double v = convertToDouble(u, bucket);
                if (v < min) {
                    v = min;
                }
                if (v > max) {
                    v = max;
                }
                int result = bucket.preSum;
                if (max > min) {
                    result += (v - min) * bucket.count / (max - min);
                    if (v > min) {
                        result -= bucket.count / bucket.ndv;
                        if (result < 0) {
                            result = 0;
                        }
                    }
                }
                return result;
            }
        }
    }

    private Double convertToDouble(Object key, Bucket bucket) {
        switch (dataType.getSqlType()) {
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
        case Types.FLOAT:
        case Types.REAL:
        case Types.DOUBLE:
        case Types.NUMERIC:
        case Types.DECIMAL:
        case DataType.MEDIUMINT_SQL_TYPE:
        case DataType.YEAR_SQL_TYPE:
            if (dataType.convertFrom(key) == null) {
                return Double.valueOf(0);
            }
            return Double.valueOf(dataType.convertFrom(key).toString());
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
            Object lower = dataType.convertFrom(bucket.lower);
            if (lower == null) {
                lower = "";
            }
            Object upper = dataType.convertFrom(bucket.upper);
            if (upper == null) {
                upper = "";
            }
            int len = commonPrefixLen(lower.toString(), upper.toString());
            String stringKey = dataType.convertFrom(key).toString();
            //if key is out of bucket, set to lower bound
            if (dataType.compare(key, bucket.lower) < 0) {
                stringKey = lower.toString();
            }
            long longValue = 0;
            final int maxStep = 8;
            int curStep = 0;
            for (int i = len; i < stringKey.length() && i < len + maxStep; i++) {
                longValue = (longValue << 8) + stringKey.charAt(i);
                curStep++;
            }
            if (curStep < maxStep) {
                longValue <<= (maxStep - curStep) * 8;
            }
            return new Double(longValue);
        case Types.DATE:
            if (dataType.convertFrom(key) == null) {
                return new Double(0);
            }
            return Double.valueOf(Date.valueOf(dataType.convertFrom(key).toString()).getTime());
        case Types.TIME:
            if (dataType.convertFrom(key) == null) {
                return new Double(0);
            }
            return Double.valueOf(Time.valueOf(dataType.convertFrom(key).toString()).getTime());
        case Types.TIMESTAMP:
        case Types.TIME_WITH_TIMEZONE:
        case Types.TIMESTAMP_WITH_TIMEZONE:
        case DataType.DATETIME_SQL_TYPE:
            if (dataType.convertFrom(key) == null) {
                return new Double(0);
            }
            return Double.valueOf(Timestamp.valueOf(dataType.convertFrom(key).toString()).getTime());
        case Types.BIT:
        case Types.BLOB:
        case Types.CLOB:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            // FIXME to support byte type histogram
            return new Double(0);
        case Types.BOOLEAN:
            if (key instanceof Boolean) {
                return (Boolean) key ? new Double(1) : new Double(0);
            }
        default:
            return new Double(0);
        }
    }

    private int commonPrefixLen(String a, String b) {
        int len = 0;
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                len++;
            } else {
                break;
            }
        }
        return len;
    }

    private int lessEqualCount(Object u) {
        int lessCount = lessCount(u);
        Bucket bucket = findBucket(u);
        if (bucket == null) {
            return lessCount;
        } else {
            if (dataType.compare(u, bucket.lower) < 0) {    //u is not covered by bucket
                return lessCount;
            }
            return lessCount + bucket.count / bucket.ndv;
        }
    }

    private int greatCount(Object l) {
        int lessEqualCount = lessEqualCount(l);
        return totalCount() - lessEqualCount;
    }

    private int greatEqualCount(Object l) {
        int greatCount = greatCount(l);
        Bucket bucket = findBucket(l);
        if (bucket == null) {
            return greatCount;
        } else {
            if (dataType.compare(l, bucket.lower) < 0) {    //u is not covered by bucket
                return greatCount;
            }
            return greatCount + bucket.count / bucket.ndv;
        }
    }

    public static String serializeToJson(Histogram histogram) {
        if (histogram == null) {
            return "";
        }
        String type = StatisticUtils.encodeDataType(histogram.dataType);
        JSONObject histogramJson = new JSONObject();
        histogramJson.put("type", type);
        histogramJson.put("maxBucketSize", histogram.maxBucketSize);
        histogramJson.put("sampleRate", histogram.sampleRate);
        JSONArray bucketsJsonArray = new JSONArray();
        histogramJson.put("buckets", bucketsJsonArray);

        for (Bucket bucket : histogram.buckets) {
            JSONObject bucketJson = new JSONObject();
            bucketJson.put("count", bucket.count);
            bucketJson.put("ndv", bucket.ndv);
            bucketJson.put("preSum", bucket.preSum);
            if (type.equalsIgnoreCase("String")
                || type.equalsIgnoreCase("Timestamp")
                || type.equalsIgnoreCase("Time")
                || type.equalsIgnoreCase("Date")) {
                bucketJson.put("upper", bucket.upper.toString());
                bucketJson.put("lower", bucket.lower.toString());
            } else {
                bucketJson.put("upper", bucket.upper);
                bucketJson.put("lower", bucket.lower);
            }
            bucketsJsonArray.add(bucketJson);
        }
        return histogramJson.toJSONString();
    }

    public static Histogram deserializeFromJson(String json) {
        try {
            JSONObject histogramJson = JSON.parseObject(json);
            String type = histogramJson.getString("type");
            DataType datatype = StatisticUtils.decodeDataType(type);
            Histogram histogram = new Histogram(histogramJson.getIntValue("maxBucketSize"), datatype,
                histogramJson.getFloatValue("sampleRate"));
            JSONArray jsonArray = histogramJson.getJSONArray("buckets");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject bucketJson = jsonArray.getJSONObject(i);
                Bucket bucket = new Histogram.Bucket();
                bucket.lower = bucketJson.get("lower");
                bucket.upper = bucketJson.get("upper");
                bucket.count = bucketJson.getIntValue("count");
                bucket.preSum = bucketJson.getIntValue("preSum");
                bucket.ndv = bucketJson.getIntValue("ndv");
                histogram.buckets.add(bucket);
            }
            return histogram;
        } catch (Throwable e) {
            logger.error("deserializeFromJson error ", e);
            return null;
        }
    }

    public static class Bucket {
        private Object lower;
        private Object upper;
        private int count;
        private int preSum;
        private int ndv;

        public Object getLower() {
            return lower;
        }

        public Object getUpper() {
            return upper;
        }

        public int getCount() {
            return count;
        }

        public int getPreSum() {
            return preSum;
        }

        public int getNdv() {
            return ndv;
        }
    }

}
