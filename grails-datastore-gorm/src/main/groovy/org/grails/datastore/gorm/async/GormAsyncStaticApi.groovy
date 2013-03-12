/* Copyright (C) 2013 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.async

import org.grails.datastore.gorm.GormStaticApi

/**
 * Transforms the GormStaticApi into an asynchronous API
 *
 * @author Graeme Rocher
 * @since 2.3
 */
class GormAsyncStaticApi<D> {
    @grails.async.DelegateAsync GormStaticApi<D> staticApi

    GormAsyncStaticApi(GormStaticApi<D> staticApi) {
        this.staticApi = staticApi
    }
}