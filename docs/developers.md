# Developers


## Profiling

```bash
echo PPKT="/path/to/phenopacket.json"
java -XX:StartFlightRecording=disk=true,dumponexit=true,filename=maxodiff_profile2.jfr,settings=profile \
     -jar maxodiff-cli/target/maxodiff-cli.jar \
     analyze -p $PPKT
```