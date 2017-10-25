// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.jdisc.athenz;

/**
 * @author mortent
 */
public interface AthenzIdentityProvider {

    public String getNToken();
    public String getX509Cert();
    public String domain();
    public String service();
}