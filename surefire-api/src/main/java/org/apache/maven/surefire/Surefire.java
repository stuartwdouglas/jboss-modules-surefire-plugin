package org.apache.maven.surefire;

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

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Jason van Zyl
 * @version $Id: Surefire.java 957320 2010-06-23 19:39:59Z krosenvold $
 */
public class Surefire
{
    
    private static final int SUCCESS = 0;
    private static final int NO_TESTS = 254;
    private static final int FAILURE = 255;
    
    public static final String SUREFIRE_BUNDLE_NAME = "org.apache.maven.surefire.surefire";

    // DGF backwards compatibility
    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, null,
                    Boolean.FALSE ) == 0;
    }
    
    public int run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                        ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, null,
                    failIfNoTests );
    }

    // DGF backwards compatibility
    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader,
                    results, Boolean.FALSE ) == 0;
    }
    
    public int run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results,
                    Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory( reportDefinitions, surefireClassLoader);

        RunStatistics runStatistics = reporterManagerFactory.getGlobalRunStatistics();
        if ( results != null )
        {
            runStatistics.initResultsFromProperties( results );
        }

        int totalTests = 0;

        SurefireTestSuite suite = createSuiteFromDefinition( testSuiteDefinition, surefireClassLoader, testsClassLoader );

        int testCount = suite.getNumTests();
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        if ( totalTests == 0 )
        {
            reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
        }
        else
        {
            suite.execute( testSetName, reporterManagerFactory, testsClassLoader );
        }

        reporterManagerFactory.close();

        if ( results != null )
        {
            runStatistics.updateResultsProperties( results );
        }

        if ( failIfNoTests.booleanValue() )
        {
            if ( runStatistics.getCompletedCount() == 0 )
            {
                return NO_TESTS;
            }
        }
        

        return runStatistics.isProblemFree() ? SUCCESS : FAILURE;

    }

    public boolean run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                        ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        return run ( reportDefinitions, testSuiteDefinitions, surefireClassLoader, testsClassLoader, Boolean.FALSE )
               == 0;
    }
    public int run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                        ClassLoader testsClassLoader, Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory( reportDefinitions, surefireClassLoader);

        RunStatistics runStatistics = reporterManagerFactory.getGlobalRunStatistics();

        List suites = new ArrayList();

        int totalTests = 0;
        for ( Iterator i = testSuiteDefinitions.iterator(); i.hasNext(); )
        {
            Object[] definition = (Object[]) i.next();

            SurefireTestSuite suite = createSuiteFromDefinition( definition, surefireClassLoader, testsClassLoader );

            int testCount = suite.getNumTests();
            if ( testCount > 0 )
            {
                suites.add( suite );
                totalTests += testCount;
            }
        }

        if ( totalTests == 0 )
        {
            reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
        }
        else
        {
            for ( Iterator i = suites.iterator(); i.hasNext(); )
            {
                SurefireTestSuite suite = (SurefireTestSuite) i.next();
                suite.execute( reporterManagerFactory, testsClassLoader );
            }
        }

        reporterManagerFactory.close();
        if ( failIfNoTests.booleanValue() )
        {
            if ( runStatistics.getCompletedCount() == 0 )
            {
                return NO_TESTS;
            }
        }

        return runStatistics.isProblemFree() ? SUCCESS : FAILURE;
    }

    private SurefireTestSuite createSuiteFromDefinition( Object[] definition, ClassLoader surefireClassLoader,
                                                         ClassLoader testsClassLoader )
        throws TestSetFailedException
    {
        String suiteClass = (String) definition[0];
        Object[] params = (Object[]) definition[1];

        SurefireTestSuite suite = instantiateSuite( suiteClass, params, surefireClassLoader );

        suite.locateTestSets( testsClassLoader );

        return suite;
    }

    public static Object instantiateObject( String className, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException, ClassNotFoundException, NoSuchMethodException
    {
        Class clazz = classLoader.loadClass( className );

        Object object;
        try
        {
            if ( params != null )
            {
                Class[] paramTypes = new Class[params.length];

                for ( int j = 0; j < params.length; j++ )
                {
                    if ( params[j] == null )
                    {
                        paramTypes[j] = String.class;
                    }
                    else
                    {
                        paramTypes[j] = params[j].getClass();
                    }
                }

                Constructor constructor = clazz.getConstructor( paramTypes );

                object = constructor.newInstance( params );
            }
            else
            {
                object = clazz.newInstance();
            }
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        return object;
    }

    private static SurefireTestSuite instantiateSuite( String suiteClass, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return (SurefireTestSuite) instantiateObject( suiteClass, params, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "Unable to find class to create suite '" + suiteClass + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException(
                "Unable to find appropriate constructor to create suite: " + e.getMessage(), e );
        }
    }

}