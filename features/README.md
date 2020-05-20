### JSONRPC features

Here is brief description of JSONRPC features

##### odl-jsonrpc-bus

Includes transport protocol implementations (http/ws/zmq), message library and JSONRPC codec

##### odl-jsonrpc-provider

JSONRPC mountpoint provider suited for non-clustered deployments. Has dependency on `odl-jsonrpc-bus` and is mutaully exclusive with `odl-jsonrpc-cluster`

##### odl-jsonrpc-all

Includes `odl-jsonrpc-provider` and all optional dependencies such as RESTConf apidoc explorer

##### odl-jsonrpc-cluster

JSONRPC mountpoint provider suitable for clustered deployments. Has dependency on `odl-jsonrpc-bus` and is mutually exclusive with `odl-jsonrpc-provider`