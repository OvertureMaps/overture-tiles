Instructions for AWS:

```
mkdir -p /mnt/ephemeral
mkfs -t ext4 /dev/nvme1n1
mount -o discard,defaults,noatime /dev/nvme1n1 /mnt/ephemeral
chmod a+rw -R /mnt/ephemeral
```
