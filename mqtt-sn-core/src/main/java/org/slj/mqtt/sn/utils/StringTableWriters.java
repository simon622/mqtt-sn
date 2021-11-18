/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.utils;

import java.util.Iterator;
import java.util.List;

/**
 * Utilities to convert tabulated model into various output formats
 *
 * @since 01/2004
 * @author Simon Johnson <simon622 AT gmail.com>
 *
 * Contributors;
 * 2019 Matt h - improved ASCII output
 */
public class StringTableWriters {

    static final char PLUS = '+';
    static final char HYPHEN = '-';
    static final char UNDERSCORE = '_';
    static final char PIPE = '|';
    static final char WHITESPACE = ' ';
    static final String NEWLINE = System.lineSeparator();

    public static String writeStringTableAsCSV(StringTable st){
        CSVTextWriter textTable = new CSVTextWriter();
        return textTable.write(st);
    }

    public static String writeStringTableAsASCII(StringTable st){
        ASCIITableWriter writer = new ASCIITableWriter();
        return writer.write(st);
    }

    public static String writeStringTableAsHTML(StringTable st, boolean borders, int padding){
        HTMLTextWriter htmlTable = new HTMLTextWriter(borders, padding);
        return htmlTable.write(st);
    }

    private static class HTMLTextWriter {

        boolean borders = false;
        int padding;

        public HTMLTextWriter(boolean borders, int padding) {
            this.borders = borders;
            this.padding = padding;
        }

        public String write(StringTable table) {

            StringBuilder sb = new StringBuilder();

            sb.append("<table style=\"border: "+(borders ? 1 : 0)+"px solid black;\">");
            sb.append("<thead>");
            sb.append("<tr>");

            //-- headings
            List<String> headings = table.getHeadings();
            Iterator<String> itr = headings.iterator();
            while(itr.hasNext()){
                String heading = itr.next();
                sb.append("<th style=\"padding:"+padding+"px;border-bottom: 1px solid #ddd;\">");
                sb.append(heading.trim());
                sb.append("</th>");
            }

            sb.append("</tr>");
            sb.append("</thead>");

            //-- data
            List<String[]> data = table.getRows();
            if(data != null && !data.isEmpty()) {

                Iterator<String[]> dataItr = data.iterator();
                while(dataItr.hasNext()){
                    sb.append("<tr>");
                    String[] row = dataItr.next();
                    for (int i = 0; i < row.length; i++) {
                        sb.append("<td style=\"padding:"+padding+"px;border-bottom: 1px solid #ddd;\">");
                        sb.append(row[i].trim());
                        sb.append("</td>");
                    }

                    sb.append("</tr>");
                }
            }

            //-- rollup
            List<String> footers = table.getFooter();
            if(footers != null && !footers.isEmpty()) {
                Iterator<String> footerItr = footers.iterator();
                sb.append("<tr>");
                while(footerItr.hasNext()){
                    sb.append("<td>");
                    String footer = footerItr.next();
                    sb.append(footer.trim());
                    sb.append("</td>");
                }
                sb.append("</tr>");
            }

            sb.append("</table>");
            return sb.toString();
        }
    }

    private static class CSVTextWriter {

        static final char SEP = ',';
        static final String NEWLINE = System.lineSeparator();

        public String write(StringTable table) {
            return write(table, SEP);
        }

        public String write(StringTable table, char sep) {

            StringBuilder sb = new StringBuilder();

            //-- headings
            List<String> headings = table.getHeadings();
            Iterator<String> itr = headings.iterator();
            while(itr.hasNext()){
                String heading = itr.next();
                sb.append(heading.trim());
                if(itr.hasNext()) sb.append(sep);
            }

            //-- data
            List<String[]> data = table.getRows();
            if(data != null && !data.isEmpty()) {
                sb.append(NEWLINE);

                Iterator<String[]> dataItr = data.iterator();
                while(dataItr.hasNext()){
                    String[] row = dataItr.next();
                    for (int i = 0; i < row.length; i++) {
                        if(i > 0)  sb.append(sep);
                        sb.append(row[i].trim());
                    }

                    if(dataItr.hasNext()) sb.append(NEWLINE);
                }
            }

            //-- rollup
            List<String> footers = table.getFooter();
            if(footers != null && !footers.isEmpty()) {
                sb.append(NEWLINE);

                Iterator<String> footerItr = footers.iterator();
                while(footerItr.hasNext()){
                    String footer = footerItr.next();
                    sb.append(footer.trim());
                    if(footerItr.hasNext()) sb.append(sep);
                }
            }

            return sb.toString();
        }
    }

    private static class ASCIITableWriter {

        private StringTable table;
        private int rowCount;
        private int columnCount;
        private int[] columnWidths;
        private StringBuilder text;

        private String write(StringTable table) {
            // Initialise
            this.table = table;
            this.rowCount = table.getRows().size();
            this.columnCount = StringTable.getColumnCount(table);

            // Calculate column positions.
            columnWidths = getColumnWidths();

            // Write table.
            return writeTable();
        }

        private int[] getColumnWidths() {
            int[] columnWidths = new int[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnWidths[i] = getColumnWidth(i);
            }
            return columnWidths;
        }

        private int getColumnWidth(int columnIndex) {
            int columnWidth = StringTable.getColumnName(table, columnIndex).length();
            for (int i = 0; i < rowCount; i++) {
                int valueWidth = table.getCellValue(i, columnIndex).length();
                columnWidth = Math.max(columnWidth, valueWidth);
            }
            if (null != table.getFooter() && table.getFooter().size() == columnCount) {
                String footer = table.getFooter().get(columnIndex);
                if (null != footer && footer.length() > columnWidth) {
                    columnWidth = footer.length();
                }
            }
            return columnWidth;
        }

        private String writeTable() {
            text = new StringBuilder();

            if (null != table.getTableName()) {
                text.append(table.getTableName().toUpperCase());
                writeLine();
            }

            writeSeparatorRow();
            writeColumnNames();
            writeSeparatorRow();
            for (int i = 0; i < rowCount; i++) {
                writeRow(i);
            }
            writeSeparatorRow();

            if (null != table.getFooter() && table.getFooter().size() == columnCount) {
                for (int i = 0; i < columnCount; i++) {
                    writeValue(table.getFooter().get(i), i);
                }
                writeLine();
                writeSeparatorRow();
            }

            return text.toString();
        }

        private void writeSeparatorRow() {
            StringBuilder sb = new StringBuilder();
            for (int width : columnWidths) {
                for (int i = 0; i < width + 4; i++) {
                    sb.append((i == 0 || i == width + 3) ? PLUS : HYPHEN);
                }
            }
            text.append(sb);
            writeLine();
        }

        private void writeColumnNames() {
            for (int i = 0; i < columnCount; i++) {
                writeValue(StringTable.getColumnName(table, i), i);
            }
            writeLine();
        }

        private void writeRow(int rowIndex) {
            for (int i = 0; i < columnCount; i++) {
                writeValue(table.getCellValue(rowIndex, i), i);
            }
            writeLine();
        }

        private void writeValue(String value, int columnIndex) {
            StringBuilder sb = new StringBuilder("| ").append(value);
            while (sb.length() < columnWidths[columnIndex] + 2) {
                sb.append(WHITESPACE);
            }
            sb.append(" |");
            text.append(sb);
        }

        private void writeLine() {
            text.append(NEWLINE);
        }
    }
}
