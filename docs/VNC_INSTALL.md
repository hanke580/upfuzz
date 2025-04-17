# Install VNC

```bash
# install
sudo apt update
sudo apt install tightvncserver

# set up vnc
tightvncserver :1

# set passwork to 1234qwer
```

# Port Forwarding

```bash
ssh username@mufasa.cs.purdue.edu -L 9901:localhost:5900

# 9901: local host
# 5900: remote host	

export TESTING_SERVER=Tingjia@node0.ubuntu22-test1.advosuwmadison-pg0.wisc.cloudlab.us
ssh  ${TESTING_SERVER} -L 9901:localhost:59010
```

Sometimes need to install the desktop

```bash
# Install the Full Desktop Environment
#Sometimes, the base package of a desktop environment doesn't include all components #necessary for a complete desktop experience. Ensure you have the full desktop environment installed. For XFCE, you can ensure this by installing xfce4 and xfce4-goodies:

sudo apt-get update
sudo apt-get install xfce4 xfce4-goodies
```

# visualVM

```bash
sudo apt update
sudo apt install visualvm

# Run
visualvm

```

