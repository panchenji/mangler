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

#ifndef _MANGLERCONFIG_H
#define _MANGLERCONFIG_H

class ManglerServerConfig/*{{{*/
{
    public:
        std::string         name;
        std::string         port;
        std::string         username;
        std::string         password;
        std::string         phonetic;
        std::string         comment;
        std::string         url;

        ManglerServerConfig() {
        }
};/*}}}*/
class ManglerConfig {
	public:
		Glib::Mutex     mutex;
		uint32_t        lv3_debuglevel;
		bool            PushToTalkKeyEnabled;
		std::string     PushToTalkKeyValue;
		bool            PushToTalkMouseEnabled;
		std::string     PushToTalkMouseValue;
		ManglerServerConfig   qc_lastserver;
		std::vector<ManglerServerConfig> serverlist;

		ManglerConfig();
		void save();
		std::string get(std::string cfgname);
		void load();
};

#endif