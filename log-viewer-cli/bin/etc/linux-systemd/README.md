# Systemd Configuration

This directory contains configuration files for running LogViewer under the
"systemd" service manager on Linux both under either a systemd system service or
systemd user service.

### How to set up a system service

1. Create the user who should run the service, or choose an existing one.
2. Copy the `log-viewer/etc/linux-systemd/system/log-viewer.service` file into the [load path of the system instance][1].
3. Change the path to LogViewer and Java in the `log-viewer.service`. [See the editing section](#editing-the-service).
4. Enable and start the service:

```
sudo systemctl enable log-viewer.service
sudo systemctl start log-viewer.service
```

### How to set up a user service

1. Create the user who should run the service, or choose an existing one.  
*Probably this will be your own user account.*
2. Copy the `log-viewer/etc/linux-systemd/user/log-viewer.service` file into the [load path of the system instance][1]. To do this without root privileges you can just use this folder under your home directory: `~/.config/systemd/user/`. 
3. Change the path to LogViewer and Java in the `log-viewer.service`. [See the editing section](#editing-the-service).
4. Enable and start the service:

```
systemctl --user enable log-viewer.service
systemctl --user start log-viewer.service
```

4. If your home directory is encrypted with eCryptfs on Debian/Ubuntu, then you will need to make the change described in [Ubuntu bug 1734290][2]. Otherwise the user service will not start, because by default, systemd checks for user services before your home directory has been decrypted.

Automatic start-up of systemd user instances at boot (before login) is possible through systemd’s “lingering” function, if a system service cannot be used instead. Refer to the [enable-linger][3] command of loginctl to allow this for a particular user.

### Checking the service status

To check if LogViewer runs properly you can use the status subcommand. To check the status of a system service:

```
sudo systemctl status log-viewer.service
```

To check the status of a user service:

```
systemctl --user status log-viewer.service
```

### Editing the service

To edit the system service, run:

```
sudo systemctl edit --full log-viewer.service
```

To edit the user service, run:

```
systemctl --user edit --full log-viewer.service
```

[1]: https://www.freedesktop.org/software/systemd/man/systemd.unit.html#Unit%20File%20Load%20Path
[2]: https://bugs.launchpad.net/ecryptfs/+bug/1734290
[3]: https://www.freedesktop.org/software/systemd/man/loginctl.html#enable-linger%20USER…