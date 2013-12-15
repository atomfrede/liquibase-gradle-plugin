/*
 * Copyright 2011 Tim Berglund
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
 *
 * Tim Berglund
 * The August Technology Group
 * http://augusttechgroup.com/tim/about
 *
 */

package com.augusttechgroup.gradle.liquibase

import org.gradle.api.Project
import org.gradle.api.Plugin
import com.augusttechgroup.gradle.liquibase.tasks.LiquibaseBaseTask
import com.augusttechgroup.gradle.liquibase.tasks.LiquibaseDbDocTask
import com.augusttechgroup.gradle.liquibase.tasks.LiquibaseDiffTask


class LiquibasePlugin
  implements Plugin<Project> {


  void apply(Project project) {
    applyExtension(project)
    applyTasks(project)
  }


  void applyExtension(Project project) {
    def databases = project.container(Database) { name -> 
      new Database(name) 
    }
    def changelogs = project.container(ChangeLog) { name -> 
      new ChangeLog(name) 
    }
    project.configure(project) {
      extensions.create("liquibase", LiquibaseExtension, databases, changelogs)
    }
  }

  
  void applyTasks(Project project) {
	  // parse the system properties once and use the resulting options for all
	  // tasks.

	  // Create basic tasks that don't take a value.
	  def liquibaseOptions = parseOptions()
    [
      'status': 'Outputs count (list if --verbose) of unrun change sets.',
			'validate': 'Checks the changelog for errors.',
			'changelogSync': 'Mark all changes as executed in the database.',
			'changelogSyncSQL': 'Writes SQL to mark all changes as executed in the database to STDOUT.',
      'listLocks': 'Lists who currently has locks on the database changelog.',
			'releaseLocks': 'Releases all locks on the database changelog.',
			'markNextChangesetRan': 'Mark the next change set as executed in the database.',
      'markNextChangesetRanSQL': 'Writes SQL to mark the next change set as executed in the database to STDOUT.',
			'dropAll': 'Drops all database objects owned by the user. Note that functions, procedures and packages are not dropped (limitation in 1.8.1).',
			'clearChecksums': 'Removes current checksums from database. On next run checksums will be recomputed.',
      'generateChangelog': 'generateChangeLog of the database to standard out. v1.8 requires the dataDir parameter currently.',
      'futureRollbackSQL': 'Writes SQL to roll back the database to the current state after the changes in the changeslog have been applied.',
	    'update': 'Updates the database to the current version.',
	    'updateSQL': 'Writes SQL to update the database to the current version to STDOUT.',
			'updateTestingRollback': 'Updates the database, then rolls back changes before updating again.'
    ].each { taskName, taskDescription ->
      project.task(taskName, type: LiquibaseBaseTask) {
        group = 'Liquibase'
	      description = taskDescription
	      options = liquibaseOptions
	      command = taskName
      }
    }

	  // Create basic tasks that do take a value.
	  [
	    'updateCount': 'Applies the next <liquibase.commandValue> change sets.',
			'updateCountSql' : 'Writes SQL to apply the next <liquibase.commandValue> change sets to STDOUT.',
			'tag': 'Tags the current database state with <liquibase.commandValue> for future rollback',
		  'rollback' : 'Rolls back the database to the state it was in when the <liquibase.commandValue> tag was applied.',
		  'rollbackToDate' : 'Rolls back the database to the state it was in at the <liquibase.commandValue> date/time.',
		  'rollbackCount' : 'Rolls back the last <liquibase.commandValue> change sets.',
			'rollbackSQL' : 'Writes SQL to roll back the database to the state it was in when the <liquibase.commandValue> tag was applied to STDOUT.',
			'rollbackToDateSQL' : 'Writes SQL to roll back the database to the state it was in at the <liquibase.commandValue> date/time to STDOUT.',
			'rollbackCountSQL' : 'Writes SQL to roll back the last <liquibase.commandValue> change sets to STDOUT.'
	  ].each { taskName, taskDescription ->
      project.task(taskName, type: LiquibaseBaseTask) {
        group = 'Liquibase'
	      description = taskDescription
	      options = liquibaseOptions
	      command = taskName
	      value = System.properties['liquibase.commandValue']
      }
	  }

	  // Create the diff task
    project.task('diff', type: LiquibaseDiffTask) {
      group = 'Liquibase'
	    description = 'Writes description of differences to standard out.'
	    options = liquibaseOptions
	    command = 'diff'
    }

	  // Create the dbDoc task
    project.task('dbDoc', type: LiquibaseDbDocTask) {
	    group = 'Liquibase'
	    description = 'Generates Javadoc-like documentation based on current database and change log'
	    options = liquibaseOptions
	    command = 'dbDoc'
	    docDir = project.file("${project.buildDir}/database/docs")
    }
  }

	/**
	 * Parse the system properties that start with "liquibase."  What comes after
	 * the dot is assumed to be a liquibase option name.  At the moment, all
	 * options require a value, so we just translate "-Dliquibase.something=value"
	 * to "--something=value" and add that to options that are passed to all
	 * commands. We have a special exclude for "liquibase.commandValue" which is used
	 * to pass a value to a command.
	 * @return the
	 */
	def parseOptions() {
		def options = []
		System.properties.findAll { it.key.startsWith("liquibase.") }.each { k, v ->
			if ( k != "liquibase.commandValue") {
				options << "--${k.substring(10)}=${v}"
			}
		}
		return options
	}

}

