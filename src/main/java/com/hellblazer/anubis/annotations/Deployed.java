/**
 * Copyright (C) 2010 Hal Hildebrand. All rights reserved. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.hellblazer.anubis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Deployed annotation is used on a method that needs to be executed 
 * after dependency injection is done, but before post construction startup.
 * The Deployed lifecycle state is missing from the Spring lifecycle and
 * is used to provide any post construction configuration that must occur
 * before active systems start up.
 * 
 * <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a> 
 *
 */
@Target( { ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Deployed {

}
