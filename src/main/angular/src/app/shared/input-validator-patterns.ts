/*
Copyright (C) 2017 Axel Müller <axel.mueller@avanux.de>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

export class InputValidatorPatterns {
  static INTEGER = '\\d*';
  static FLOAT = '^\\d*(\\.\\d+)?$';
  // https://regexr.com/3h7bc
  static URL = '(https?|ftp):\\/\\/([-A-Za-z0-9:%_.]{3,}@)?((([-a-z0-9.]{2,256})(\\.[a-z]{2,4}){1})|((?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))(\\:[0-9]*)?(\\/[-a-zA-Z0-9\\(\\)@:%,_\\+.~#?&//=]*)?';
}
