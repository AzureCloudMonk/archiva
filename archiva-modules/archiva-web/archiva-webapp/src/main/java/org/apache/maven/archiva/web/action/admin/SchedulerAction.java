package org.apache.maven.archiva.web.action.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="schedulerAction" instantiation-strategy="per-lookup"
 */
public class SchedulerAction
    extends PlexusActionSupport
    implements SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaTaskScheduler taskScheduler;

    private String repoid;
    
    private boolean scanAll;
    
    public String scanRepository()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Cannot run indexer on blank repository id." );
            return SUCCESS;
        }

        RepositoryTask task = TaskCreator.createRepositoryTask( repoid, scanAll ); 
        
        if ( taskScheduler.isProcessingRepositoryTask( repoid ) )
        {
            addActionError( "Repository [" + repoid + "] task was already queued." );
        }
        else
        {
            try
            {
                addActionMessage( "Your request to have repository [" + repoid + "] be indexed has been queued." );
                taskScheduler.queueRepositoryTask( task );                
            }
            catch ( TaskQueueException e )
            {
                addActionError( "Unable to queue your request to have repository [" + repoid + "] be indexed: "
                    + e.getMessage() );
            }
        }

        // Return to the repositories screen.
        return SUCCESS;
    }

    public String updateDatabase()
    {
        log.info( "Queueing database task on request from user interface" );
        DatabaseTask task = new DatabaseTask();

        if ( taskScheduler.isProcessingDatabaseTask() )
        {
            addActionError( "Database task was already queued." );
        }
        else
        {
            try
            {
                taskScheduler.queueDatabaseTask( task );
                addActionMessage( "Your request to update the database has been queued." );
            }
            catch ( TaskQueueException e )
            {
                addActionError( "Unable to queue your request to update the database: " + e.getMessage() );
            }
        }

        // Return to the database screen.
        return SUCCESS;
    }

    @Override
    public void addActionMessage( String aMessage )
    {
        super.addActionMessage( aMessage );
        log.info( "[ActionMessage] " + aMessage );
    }

    @Override
    public void addActionError( String anErrorMessage )
    {
        super.addActionError( anErrorMessage );
        log.warn( "[ActionError] " + anErrorMessage );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_RUN_INDEXER, Resource.GLOBAL );

        return bundle;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
    
    public boolean getScanAll()
    {
        return scanAll;
    }

    public void setScanAll( boolean scanAll )
    {
        this.scanAll = scanAll;
    }
}