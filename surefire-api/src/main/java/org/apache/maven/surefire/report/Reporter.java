package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Contract between the different implementations of the Surefire reporters
 *
 * @version $Id: Reporter.java 957320 2010-06-23 19:39:59Z krosenvold $
 */
public interface Reporter
{
    // The entire run
    void runStarting();

    void runCompleted();

    // Test Sets
    void testSetStarting( ReportEntry report )
        throws ReporterException;

    void testSetCompleted( ReportEntry report )
        throws ReporterException;

    // Tests

    /**
     * Event fired when a test is about to start
     *
     * @param report The report entry to log for
     */
    void testStarting( ReportEntry report );

    /**
     * Event fired when a test ended successfully
     *
     * @param report The report entry to log for
     */
    void testSucceeded( ReportEntry report );

    /**
     * Event fired when a test ended with an error (non anticipated problem)
     *
     * @param report The report entry to log for
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testError( ReportEntry report, String stdOut, String stdErr );

    /**
     * Event fired when a test ended with a failure (anticipated problem)
     *
     * @param report The report entry to log for
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testFailed( ReportEntry report, String stdOut, String stdErr );

    void testSkipped( ReportEntry report );

    // Counters
    void reset();

    void writeMessage( String message );

    void writeFooter( String footer );

}