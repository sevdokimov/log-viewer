# Upstart Configuration

This directory contains example configuration files for running LogViewer under
the "Upstart" service manager on Linux.

### How to set up a system service

1. Copy the `log-viewer/etc/linux-upstart/system/log-viewer.conf` file into the `/etc/init/`.
2. Change the path to LogViewer and Java in the `/etc/init/log-viewer.conf`.
3. Create a link on file:
```
ln -s /lib/init/upstart-job /etc/init.d/log-viewer
```
4. Start the service:

```
sudo service start log-viewer
```
Or
```
sudo initctl start log-viewer
```

### How to set up a user service

1. Copy the `log-viewer/etc/linux-upstart/user/log-viewer.conf` file into the `/home/[username]/.config/upstart/`.
2. Change the path to LogViewer and Java in the `/home/[username]/.config/upstart/log-viewer.conf`.
3. Log Viewer will run when you log in.

### Checking the service status
To check if LogViewer runs properly you can use the status subcommand:

```
sudo service status log-viewer
```
Or
```
sudo initctl status log-viewer
```