/*
 * vim: softtabstop=4 shiftwidth=4 cindent foldmethod=marker expandtab
 *
 * $LastChangedDate: 2009-10-10 12:38:51 -0700 (Sat, 10 Oct 2009) $
 * $Revision: 63 $
 * $LastChangedBy: ekilfoil $
 * $URL: http://svn.mangler.org/mangler/trunk/src/manglersettings.h $
 *
 * Copyright 2009 Eric Kilfoil 
 *
 * This file is part of Mangler.
 *
 * Mangler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mangler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mangler.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef _MANGLERCHAT_H
#define _MANGLERCHAT_H

class ManglerChat {
    public:
       ManglerChat(Glib::RefPtr<Gtk::Builder> builder); 
       
       Gtk::Window   *chatWindow;
       Gtk::Button   *button;
       Gtk::Entry    *chatMessage;
       Gtk::TextView *chatBox;
       
       void chatWindow_show_cb(void);
       void chatWindow_hide_cb(void);
       void chatWindowSendChat_clicked_cb(void);
       
       void AddMessage(Glib::ustring username, Glib::ustring message);
};

#endif