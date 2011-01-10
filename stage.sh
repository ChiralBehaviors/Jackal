for host in 0 1 3 4 5 
do
HOST=g$host
ssh $HOST 'rm -r hal;mkdir hal'
scp staging.jar $HOST:hal
ssh $HOST 'cd hal; jar -xf staging.jar; chmod +x *.sh'
done

for host in 1 2 3 4 
do
HOST=v$host
ssh $HOST 'rm -r hal;mkdir hal'
scp staging.jar $HOST:hal
ssh $HOST 'cd hal; jar -xf staging.jar; chmod +x *.sh'
done