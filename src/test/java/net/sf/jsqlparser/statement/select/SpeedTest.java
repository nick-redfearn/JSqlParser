/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.select;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.simpleparsing.CCJSqlParserManagerTest;
import net.sf.jsqlparser.test.TestException;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SpeedTest {

    private final static int NUM_REPS_500 = 500;
    private final CCJSqlParserManager parserManager = new CCJSqlParserManager();

    @Test
    @Disabled
    // replaced by a proper JMH based benchmark
    // @todo: remove this eventually
    public void testSpeed() throws Exception {
        // all the statements in testfiles/simple_parsing.txt
        BufferedReader in = new BufferedReader(
                new InputStreamReader(SpeedTest.class.getResourceAsStream("/simple_parsing.txt")));
        CCJSqlParserManagerTest d;
        List<String> statementsList = new ArrayList<>();

        while (true) {
            String statement = CCJSqlParserManagerTest.getStatement(in);
            if (statement == null) {
                break;
            }
            statementsList.add(statement);
        }
        in.close();
        in = new BufferedReader(new InputStreamReader(
                SpeedTest.class.getResourceAsStream("/RUBiS-select-requests.txt")));

        // all the statements in testfiles/RUBiS-select-requests.txt
        while (true) {
            String line = CCJSqlParserManagerTest.getLine(in);
            if (line == null) {
                break;
            }
            if (line.length() == 0) {
                continue;
            }

            if (!line.equals("#begin")) {
                break;
            }
            line = CCJSqlParserManagerTest.getLine(in);
            StringBuilder buf = new StringBuilder(line);
            while (true) {
                line = CCJSqlParserManagerTest.getLine(in);
                if (line.equals("#end")) {
                    break;
                }
                buf.append("\n");
                buf.append(line);
            }
            if (!CCJSqlParserManagerTest.getLine(in).equals("true")) {
                continue;
            }

            statementsList.add(buf.toString());

            String cols = CCJSqlParserManagerTest.getLine(in);
            String tables = CCJSqlParserManagerTest.getLine(in);
            String whereCols = CCJSqlParserManagerTest.getLine(in);
            String type = CCJSqlParserManagerTest.getLine(in);

        }
        in.close();

        String statement;
        int numTests = 0;
        // it seems that the very first parsing takes a while, so I put it aside
        Statement parsedStm =
                parserManager.parse(new StringReader(statement = statementsList.get(0)));
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<Select> parsedSelects = new ArrayList<>(NUM_REPS_500 * statementsList.size());
        long time = System.currentTimeMillis();

        // measure the time to parse NUM_REPS times all statements in the 2 files
        for (int i = 0; i < NUM_REPS_500; i++) {
            try {
                for (String s : statementsList) {
                    statement = s;
                    parsedStm = parserManager.parse(new StringReader(statement));
                    numTests++;
                    if (parsedStm instanceof Select) {
                        parsedSelects.add((Select) parsedStm);
                    }

                }
            } catch (JSQLParserException e) {
                throw new TestException("impossible to parse statement: " + statement, e);
            }
        }
        long elapsedTime = System.currentTimeMillis() - time;
        long statementsPerSecond = numTests * 1000L / elapsedTime;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(7);
        df.setMinimumFractionDigits(4);
        System.out.println(numTests + " statements parsed in " + elapsedTime + " milliseconds");
        System.out.println(" (" + statementsPerSecond + " statements per second,  "
                + df.format(1.0 / statementsPerSecond) + " seconds per statement )");

        numTests = 0;
        time = System.currentTimeMillis();
        // measure the time to get the tables names from all the SELECTs parsed before
        for (Select select : parsedSelects) {
            if (select != null) {
                numTests++;
                tablesNamesFinder.getTableList((Statement) select);
            }
        }
        elapsedTime = System.currentTimeMillis() - time;
        statementsPerSecond = numTests * 1000L / elapsedTime;
        System.out.println(numTests + " select scans for table name executed in " + elapsedTime
                + " milliseconds");
        System.out.println(" (" + statementsPerSecond + " select scans for table name per second,  "
                + df.format(1.0 / statementsPerSecond)
                + " seconds per select scans for table name)");

    }
}
