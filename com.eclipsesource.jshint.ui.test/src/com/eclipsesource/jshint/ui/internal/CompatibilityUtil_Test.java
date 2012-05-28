/*******************************************************************************
 * Copyright (c) 2012 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ralf Sternberg - initial implementation and API
 ******************************************************************************/
package com.eclipsesource.jshint.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.eclipsesource.jshint.ui.internal.builder.BuilderUtil;
import com.eclipsesource.jshint.ui.internal.preferences.EnablementPreferences;
import com.eclipsesource.jshint.ui.internal.preferences.OptionsPreferences;
import com.eclipsesource.jshint.ui.internal.preferences.PreferencesFactory;
import com.eclipsesource.jshint.ui.test.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CompatibilityUtil_Test {
  private IProject project;

  @Before
  public void setUp() throws BackingStoreException {
    Preferences workspacePreferences = PreferencesFactory.getWorkspacePreferences();
    workspacePreferences.clear();
    workspacePreferences.flush();
    project = TestUtil.createProject( "test" );
  }

  @After
  public void tearDown() {
    TestUtil.deleteProject( project );
  }

  @Test
  public void updateObsoleteBuilder() throws Exception {
    BuilderUtil.addBuilderToProject( project, TestUtil.OLD_BUILDER_ID );

    CompatibilityUtil.run();

    IFile projectFile = project.getFile( "/.project" );
    assertTrue( TestUtil.readContent( projectFile ).contains( TestUtil.BUILDER_ID ) );
    assertFalse( TestUtil.readContent( projectFile ).contains( TestUtil.OLD_BUILDER_ID ) );
  }

  @Test
  public void updateObsoletePrefs() throws Exception {
    TestUtil.createExampleSettingsFile( project, TestUtil.OLD_SETTINGS_FILE );

    CompatibilityUtil.run();

    IFolder settingsFolder = project.getFolder( TestUtil.SETTINGS_FOLDER_PATH );
    assertFalse( settingsFolder.getFile( TestUtil.OLD_SETTINGS_FILE ).exists() );
    assertTrue( settingsFolder.getFile( TestUtil.NEW_SETTINGS_FILE ).exists() );
    String newContent = TestUtil.readContent( settingsFolder.getFile( TestUtil.NEW_SETTINGS_FILE ) );
    assertTrue( newContent.contains( "enabled=true" ) );
  }


  @Test
  public void fallbackToOldPrefs() throws Exception {
    TestUtil.createExampleSettingsFile( project, TestUtil.OLD_SETTINGS_FILE );

    CompatibilityUtil.run();

    Preferences node = PreferencesFactory.getProjectPreferences( project );
    EnablementPreferences enablePrefs = new EnablementPreferences( node );
    assertTrue( enablePrefs.getExcludePatterns().contains( "js/test.js" ) );
    assertTrue( enablePrefs.getExcludePatterns().contains( "target" ) );
    OptionsPreferences optionsPrefs = new OptionsPreferences( node );
    assertEquals( "org: true, com: false", optionsPrefs.getGlobals() );
    assertEquals( "bitwise: true, curly: true, eqnull: true", optionsPrefs.getOptions() );
  }

  @Test
  public void getPluginVersion() {
    assertTrue( CompatibilityUtil.getPluginVersion().contains( "." ) );
  }

  @Test
  public void addsVersionToPreferences() throws Exception {
    CompatibilityUtil.run();

    Preferences wsPrefs = PreferencesFactory.getWorkspacePreferences();
    String currentVersion = CompatibilityUtil.getPluginVersion();
    assertEquals( currentVersion, wsPrefs.get( CompatibilityUtil.KEY_PLUGIN_VERSION, null ) );
  }

}