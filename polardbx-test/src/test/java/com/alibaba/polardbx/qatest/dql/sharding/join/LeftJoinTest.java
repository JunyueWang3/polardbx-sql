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

package com.alibaba.polardbx.qatest.dql.sharding.join;

import com.alibaba.polardbx.qatest.ReadBaseTestCase;
import com.alibaba.polardbx.qatest.data.ColumnDataGenerator;
import com.alibaba.polardbx.qatest.data.ExecuteTableName;
import com.alibaba.polardbx.qatest.data.ExecuteTableSelect;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.alibaba.polardbx.qatest.dql.sharding.join.JoinUtils.bkaHint;
import static com.alibaba.polardbx.qatest.dql.sharding.join.JoinUtils.bkaWithoutHashJoinHint;
import static com.alibaba.polardbx.qatest.validator.DataValidator.selectContentSameAssert;
import static com.alibaba.polardbx.qatest.validator.DataValidator.selectConutAssert;
import static com.alibaba.polardbx.qatest.validator.DataValidator.selectOrderAssert;

/**
 * Left Join测试
 *
 * @author zhuoxue
 * @since 5.0.1
 */

public class LeftJoinTest extends ReadBaseTestCase {

    protected ColumnDataGenerator columnDataGenerator = new ColumnDataGenerator();

    @Parameters(name = "{index}:table0={0},table1={1},hint={2}")
    public static List<String[]> prepareDate() {
        return Arrays.asList(ExecuteTableSelect
            .selectBaseOneBaseTwoWithHint());
    }

    public LeftJoinTest(String baseOneTableName, String baseTwoTableName,
                        String hint) {
        this.baseOneTableName = baseOneTableName;
        this.baseTwoTableName = baseTwoTableName;
        this.hint = hint;
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinUsingTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "using(pk) ";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinMultiUsingTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "using(pk, integer_test, varchar_test) ";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithWhereTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithUsingWhereTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "using(pk) " + " where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithCountLikeTest() {
        String sql = hint + "select count(*) from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test like ?";
        List<Object> param = new ArrayList<Object>();
        param.add(columnDataGenerator.varchar_tesLikeValueOne);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithUsingCountLikeTest() {
        String sql = hint + "select count(*) from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "using(integer_test)" + " where " + baseTwoTableName
            + ".varchar_test like ?";
        List<Object> param = new ArrayList<Object>();
        param.add(columnDataGenerator.varchar_tesLikeValueOne);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithAndTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'" + " and " + baseOneTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'";

        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection, true);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithOrTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'" + " OR " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValueTwo + "'";

        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithAndOrTest() {
        String sql = hint + "select * from " + baseTwoTableName + " left join "
            + baseOneTableName + " on " + baseTwoTableName + ".pk=" + baseOneTableName
            + ".integer_test where " + baseTwoTableName
            + ".varchar_test like '" + columnDataGenerator.varchar_tesLikeValueOne + "' and  ("
            + baseTwoTableName + ".varchar_test='" + columnDataGenerator.varchar_testValue + "' or "
            + baseOneTableName + ".varchar_test is null)";

        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithAndOrTrueTest() {

        String sql = hint + "select * from " + baseTwoTableName + " left join "
            + baseOneTableName + " on " + baseTwoTableName + ".pk=" + baseOneTableName
            + ".integer_test where " + baseTwoTableName
            + ".varchar_test like '%w%' and  ("
            + baseTwoTableName + ".varchar_test='word23' or "
            + baseOneTableName + ".varchar_test is true)";

        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithLimitTest() {
        String sql = hint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'" + " limit 1";

        selectConutAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithAliasTest() {
        String sql = hint
            + "select a.pk,a.varchar_test,a.pk,b.varchar_test from "
            + baseTwoTableName + " b left join " + baseOneTableName + " a "
            + "on b.pk=a.integer_test";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.1.28
     */
    @Test
    public void leftJoinWithAliasAsTest() {
        String sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName + " as b left join " + baseOneTableName + " as a "
            + "on b.pk=a.integer_test where a.pk = " + columnDataGenerator.pkValue;
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithOrderByTest() {
        // 左表有序，可下推
        String sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where b.varchar_test='" + columnDataGenerator.varchar_testValue
            + "' order by a.pk";
        selectOrderAssert(sql, null, mysqlConnection, tddlConnection);

        sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where b.varchar_test='" + columnDataGenerator.varchar_testValue
            + "' order by a.pk asc";
        selectOrderAssert(sql, null, mysqlConnection, tddlConnection);

        sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where b.varchar_test='" + columnDataGenerator.varchar_testValue
            + "' order by a.pk desc";
        selectOrderAssert(sql, null, mysqlConnection, tddlConnection);

        // bug修复后，执行这段代码
        // // 左表有序，可下推
        // String sql = hint
        // +
        // "select a.host_id,a.varchar_test,a.hostgroup_id,b.varchar_test from "
        // + baseTwoTableName
        // + " as b left join "
        // + baseOneTableName
        // + " as a "
        // +
        // "on b.hostgroup_id=a.hostgroup_id where b.varchar_test='hostgroupname0' order by b.hostgroup_id";
        // selectOrderAssert(sql,null, mysqlConnection, tddlConnection);
        //
        // sql = hint +
        // "select a.host_id,a.varchar_test,a.hostgroup_id,b.varchar_test from "
        // + baseTwoTableName
        // + " as b left join " + baseOneTableName + " as a "
        // +
        // "on b.hostgroup_id=a.hostgroup_id where b.varchar_test='hostgroupname0' order by b.hostgroup_id asc";
        // selectOrderAssert(sql,null, mysqlConnection, tddlConnection);
        //
        // sql = hint +
        // "select a.host_id,a.varchar_test,a.hostgroup_id,b.varchar_test from "
        // + baseTwoTableName
        // + " as b left join " + baseOneTableName + " as a "
        // +
        // "on b.hostgroup_id=a.hostgroup_id where b.varchar_test='hostgroupname0' order by b.hostgroup_id desc";
        // selectOrderAssert(sql,null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithOrderByRightOutterTest() {
        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */select a.pk,a.varchar_test,a.a.pk,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_testwhere b.varchar_test='hostgroupname0' order by a.pk";
        // selectOrderAssert(sql,null, mysqlConnection, tddlConnection);

        // 使用sort merge join，不需要临时表排序
        sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test order by a.integer_test asc , a.pk asc";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);

        // 使用sort merge join，不需要临时表排序
        sql = hint
            + "select a.pk,a.varchar_test,a.integer_test,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where b.varchar_test='" + columnDataGenerator.varchar_testValue
            + "' order by b.varchar_test desc";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);

        // 使用这个sql可以出发一个bug，暂时记录在这里，等bug修复可以恢复运行
        // sql = hint +
        // "select a.host_id,a.varchar_test,a.hostgroup_id,b.varchar_test from "
        // + baseTwoTableName
        // + " as b left join " + baseOneTableName + " as a "
        // +
        // "on b.hostgroup_id=a.hostgroup_id where b.varchar_test='hostgroupname0' order by b.hostgroup_id desc";
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithOrderLimitTest() {
        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ select a.integer_test,a.varchar_test,a.pk,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where  b.varchar_test like '" + columnDataGenerator.varchar_tesLikeValueOne
            + "' order by a.pk,b.varchar_test";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
        sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ select a.integer_test,a.varchar_test,a.pk,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where  b.varchar_test like '" + columnDataGenerator.varchar_tesLikeValueOne
            + "%' order by a.pk,b.varchar_test desc";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithGetByIndexTest() {
        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ select a.integer_test,a.varchar_test,a.pk,b.varchar_test from "
            + baseTwoTableName
            + " as b left join "
            + baseOneTableName
            + " as a "
            + "on b.pk=a.integer_test where  b.varchar_test like '" + columnDataGenerator.varchar_tesLikeValueOne
            + "' limit 0,2000 ";
        //TODO selectContentSameAssertByIndex
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);

    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithSubQueryTest() {
        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ SELECT sumId  ,b.pk  as bid from "
            + "( select  sum(pk) as sumId,integer_test from "
            + baseOneTableName
            + " where pk BETWEEN ? and ? GROUP BY integer_test ORDER BY integer_test ) as a"
            + " LEFT JOIN (SELECT SUM(pk) , pk  from "
            + baseTwoTableName
            + " where pk "
            + "BETWEEN ? and ? GROUP BY pk) as b ON a.integer_test=b.pk ORDER BY sumId DESC";
        List<Object> param = new ArrayList<Object>();
        param.add(0);
        param.add(100);
        param.add(1);
        param.add(20);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithUsingSubQueryTest() {
        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ SELECT sumId  ,b.pk  as bid from "
            + "( select  sum(pk) as sumId,integer_test from "
            + baseOneTableName
            + " where pk BETWEEN ? and ? GROUP BY integer_test ORDER BY integer_test ) as a"
            + " LEFT JOIN (SELECT SUM(pk) , pk, integer_test  from "
            + baseTwoTableName
            + " where pk "
            + "BETWEEN ? and ? GROUP BY pk) as b USING (integer_test) ORDER BY sumId DESC";
        List<Object> param = new ArrayList<Object>();
        param.add(0);
        param.add(100);
        param.add(1);
        param.add(20);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.1.19
     */
    @Test
    @Ignore("bug")
    public void leftJoinWithSubQueryAndNotEqualConditonTest() {
        if (StringUtils.containsIgnoreCase(hint, "SORT_MERGE_JOIN")) {
            // 不支持sort merge join hint
            return;
        }

        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ SELECT sumId ,name , a.integer_test as aid ,b.pk  as bid from "
            + "( select  sum(pk) as sumId,varchar_test  as name ,integer_test from "
            + baseOneTableName
            + " where pk BETWEEN ? and ? GROUP BY varchar_test ORDER BY integer_test LIMIT ?) as a"
            + " LEFT JOIN (SELECT SUM(pk) , varchar_test,pk  from "
            + baseTwoTableName
            + " where pk "
            + "BETWEEN ? and ? GROUP BY varchar_test) as b ON a.integer_test > b.pk ORDER BY sumId DESC";
        List<Object> param = new ArrayList<Object>();
        param.add(0);
        param.add(100);
        param.add(20);
        param.add(1);
        param.add(20);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.1.19
     */
    @Test
    @Ignore("Group_concat")
    public void leftJoinWithSubQueryAndNullConditonTest() {
        if (StringUtils.containsIgnoreCase(hint, "SORT_MERGE_JOIN")) {
            // 不支持sort merge join hint
            return;
        }

        String sql = hint
            + "/* ANDOR ALLOW_TEMPORARY_TABLE=True */ SELECT  group_concat(a.integer_test) as aid  from "
            + "( select  group_concat(pk) as sumId,varchar_test as name,integer_test from "
            + baseOneTableName
            + " where pk BETWEEN ? and ? GROUP BY integer_test ORDER BY integer_test LIMIT ?) as a"
            + " LEFT JOIN (SELECT group_concat(pk) , varchar_test,pk  from "
            + baseTwoTableName
            + " where pk "
            + "BETWEEN ? and ? GROUP BY varchar_test) as b ON a.integer_test = b.pk ORDER BY sumId DESC";
        List<Object> param = new ArrayList<Object>();
        param.add(0);
        param.add(100);
        param.add(20);
        param.add(1);
        param.add(20);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.1.21
     */
    @Test
    public void leftJoinInSubQuery() {

        // 测试SQL
        String sql =
            "select t.did, t.duid, t.sid from ( select d.pk did, d.integer_test duid, s.pk sid from select_base_two_"
                + ExecuteTableName.ONE_DB_ONE_TB_SUFFIX + "  d left join select_base_two_"
                + ExecuteTableName.MUlTI_DB_MUTIL_TB_SUFFIX
                + "  s on d.varchar_test = s.varchar_test where s.pk<100 and d.varchar_test like'"
                + columnDataGenerator.varchar_tesLikeValueOne + "' ) t where duid>3";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);

    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithUnionAndBKATest() {
        String sql = hint + bkaHint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    @Test
    public void leftJoinWithUnionAndBKANoHashTest() {
        String sql = hint + bkaWithoutHashJoinHint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithUnionAndBKAWhereTest() {
        String sql = hint + bkaHint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    @Test
    public void leftJoinWithUnionAndBKAWhereNoHashTest() {
        String sql = hint + bkaWithoutHashJoinHint + "select " + baseOneTableName + ".pk," + baseOneTableName
            + ".varchar_test," + baseOneTableName + ".integer_test," + baseTwoTableName
            + ".varchar_test from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test='" + columnDataGenerator.varchar_testValue + "'";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinWithCountLikeUnionAndBKATest() {
        String sql = hint + bkaHint + "select count(*) from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test like ?";
        List<Object> param = new ArrayList<Object>();
        param.add(columnDataGenerator.varchar_tesLikeValueOne);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    @Test
    public void leftJoinWithCountLikeUnionAndBKANoHashTest() {
        String sql = hint + bkaWithoutHashJoinHint + "select count(*) from " + baseTwoTableName + " left join "
            + baseOneTableName + "  " + "on " + baseTwoTableName + ".pk="
            + baseOneTableName + ".integer_test where " + baseTwoTableName
            + ".varchar_test like ?";
        List<Object> param = new ArrayList<Object>();
        param.add(columnDataGenerator.varchar_tesLikeValueOne);
        selectContentSameAssert(sql, param, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinOnConditionTest1() {
        String sql = "select a.pk, a.varchar_test, a.integer_test, b.varchar_test from " + baseOneTableName
            + " a left join " + baseTwoTableName + " b on a.pk=b.pk and a.pk = 1";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinOnConditionTest2() {
        String sql = "select a.pk, a.varchar_test, a.integer_test, b.varchar_test from " + baseOneTableName
            + " a left join " + baseTwoTableName + " b on a.pk=b.pk and b.pk = 1";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinOnConditionTest3() {
        String sql = "select a.pk, a.varchar_test, a.integer_test, b.varchar_test from " + baseOneTableName
            + " a left join " + baseTwoTableName + " b on a.pk=b.integer_test and a.pk = 1";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    /**
     * @since 5.0.1
     */
    @Test
    public void leftJoinOnConditionTest4() {

        String sql = "select a.pk, a.varchar_test, a.integer_test, b.varchar_test from " + baseOneTableName
            + " a left join " + baseTwoTableName + " b on a.pk=b.integer_test and b.integer_test = 1";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection);
    }

    @Test
    public void mulitLeftJoinWithSameShardKey() {
        String sql = "SELECT one.pk,two.pk,three.pk FROM select_base_one_multi_db_multi_tb AS one LEFT JOIN "
            + "select_base_two_multi_db_multi_tb AS two ON one.pk = two.pk LEFT JOIN "
            + "select_base_three_multi_db_multi_tb AS three ON two.pk = three.pk WHERE one.pk = 1\n";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection, true);
    }

    @Test
    public void leftJoinInnerJoinTest() {
        String sql = "select t1.pk, t2.integer_test, t3.pk from select_base_one_multi_db_multi_tb t1 " +
            "left join select_base_one_multi_db_one_tb t2 on t1.pk = t2.integer_test " +
            "inner join select_base_one_one_db_one_tb  t3 on t3.integer_test = t2.pk;";
        selectContentSameAssert(sql, null, mysqlConnection, tddlConnection, true);
    }

    @Test
    public void nestLoopJoinTestOnSmpExecutor() {
        String sql = "select t1.pk, t2.integer_test, t3.pk from select_base_one_multi_db_multi_tb t1 " +
            "left join select_base_one_multi_db_one_tb t2 on t1.pk = t2.integer_test " +
            "inner join select_base_one_one_db_one_tb  t3 on t3.integer_test = t2.pk;";
        String hint =
            "/*+TDDL:cmd_extra(ENABLE_MPP=false,ENABLE_BKA_JOIN=false,ENABLE_SORT_MERGE_JOIN=false,ENABLE_LOCAL_MODE=false, ENABLE_HASH_JOIN=false)*/";
        selectContentSameAssert(hint + sql, null, mysqlConnection, tddlConnection, true);
    }
}

