#!/bin/bash

# $1: deb name
# $2: ansible branch
# $3: environment

mkdir -p tmp-stockmind
mv stockmind-api*.deb tmp-stockmind/
cd tmp-stockmind

git clone -b $2 --single-branch git@ec2-34-253-65-62.eu-west-1.compute.amazonaws.com:gotoalberto/stockmind-deploy-ansible.git
cd stockmind-deploy-ansible

ansible-playbook \
    -i inventory.stockmind.$3 \
    stockmind-api.yml \
    --extra-vars "stockmind_deb_path=../$1"

cd
rm -rf tmp-stockmind
