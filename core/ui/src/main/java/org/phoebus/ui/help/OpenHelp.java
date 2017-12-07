/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

/** Menu entry to open help
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenHelp implements MenuEntry
{
    @Override
    public String getName()
    {
        return HelpApplication.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Help/Content";
    }

    @Override
    public Void call()
    {
        ApplicationService.findApplication(HelpApplication.NAME).create();
        return null;
    }
}