package org.openstack.atlas.adapter.exceptions;

public class StmClientConfigurationException extends Exception {
    private static final long serialVersionUID = -1197590882399930192L;

    public StmClientConfigurationException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
