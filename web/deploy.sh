#!/bin/bash

set -e
set -u

# ARG_OPTIONAL_SINGLE([opt-arg],[o],[Specify which branch of the ansible repo should be used.],[master])
# ARG_VERSION([echo deploy v$version])
# ARG_HELP([Deploy the app in one step])
# ARGBASH_GO()
# needed because of Argbash --> m4_ignore([
### START OF CODE GENERATED BY Argbash v2.3.0 one line above ###
# Argbash is a bash code generator used to get arguments parsing right.
# Argbash is FREE SOFTWARE, see https://argbash.io for more info
# Generated online by https://argbash.io/generate

die()
{
	local _ret=$2
	test -n "$_ret" || _ret=1
	test "$_PRINT_HELP" = yes && print_help >&2
	echo "$1" >&2
	exit ${_ret}
}

# THE DEFAULTS INITIALIZATION - OPTIONALS
_arg_ansible_branch="master"
version="0.2"

print_help ()
{
	echo "Deploy the app in one step."
	printf 'Usage: %s [-b|--ansible-branch <arg>] [-v|--version] [-h|--help]\n' "$0"
	printf "\t%s\n" "-b,--ansible-branch: Specify which branch of the ansible repo should be used. Default is 'master'. (default: '"master"')"
	printf "\t%s\n" "-v,--version: Prints version"
	printf "\t%s\n" "-h,--help: Prints help"
}

# THE PARSING ITSELF
while test $# -gt 0
do
	_key="$1"
	case "$_key" in
		-b|--ansible-branch|--ansible-branch=*)
			_val="${_key##--ansible-branch=}"
			if test "$_val" = "$_key"
			then
				test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
				_val="$2"
				shift
			fi
			_arg_ansible_branch="$_val"
			;;
		-v|--version)
			echo deploy v$version
			exit 0
			;;
		-h|--help)
			print_help
			exit 0
			;;
		*)
			_PRINT_HELP=yes die "FATAL ERROR: Got an unexpected argument '$1'" 1
			;;
	esac
	shift
done

# OTHER STUFF GENERATED BY Argbash

### END OF CODE GENERATED BY Argbash (sortof) ### ])

SOLIDGO_EC2_KEY=~/.ssh/prod.pem

SOLIDGO_VERSION=$( awk '/^  version :=/ {print $3}' build.sbt | sed -e 's/^"//' -e 's/",$//' )
SOLIDGO_DEB_PATH=../../../target/solidgo-api_${SOLIDGO_VERSION}_all.deb

rm -rf tmp

if [[ -f ${SOLIDGO_EC2_KEY} ]]
then

    echo ""
    echo "----------------------------------------"
    echo "Building deb file..."
    echo "----------------------------------------"
    sbt clean debian:packageBin

    echo ""
    echo "----------------------------------------"
    echo "Downloading playbook..."
    echo "----------------------------------------"
    mkdir -p tmp/workdir
    cd tmp/workdir
    git clone -b $_arg_ansible_branch --single-branch git@ec2-34-253-65-62.eu-west-1.compute.amazonaws.com:gotoalberto/stockmind-deploy-ansible.git
    cd stockmind-deploy-ansible

    echo ""
    echo "----------------------------------------"
    echo "Running playbook..."
    echo "----------------------------------------"
    ansible-playbook \
        -i inventory.stockmind.bankia-test \
        --key-file=${SOLIDGO_EC2_KEY} \
        stockmind-api.yml \
        --extra-vars "solidgo_deb_path=${SOLIDGO_DEB_PATH}"

else
    echo "EC2 key not present at ${SOLIDGO_EC2_KEY}"
    echo "Please put the private EC2 key there."
fi

echo ""
echo "----------------------------------------"
echo "Cleaning..."
echo "----------------------------------------"

cd ../../..
rm -rf tmp
