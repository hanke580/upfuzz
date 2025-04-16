# Systems

## Ozone

Architecture of Ozone is based on the following components:
```
StorageContainerManagerStarter
OzoneManagerStarter
HddsDatanodeService
```

When increasing node numbers, HddsDatanodeService will increase. E.g. if N = 5, there will be 3 HddsDatanodeService.