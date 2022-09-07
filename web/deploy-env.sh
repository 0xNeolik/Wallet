#!/bin/bash
#
# ARG_OPTIONAL_SINGLE([ansible-branch],[b],[Specify which branch of the ansible repo should be used.],[master])
# ARG_OPTIONAL_SINGLE([env],[e],[Name of the environment in which to deploy.])
# ARG_OPTIONAL_SINGLE([key-file],[i],[Location of the key needed to connect to the commander.])
# ARG_OPTIONAL_SINGLE([host],[H],[URL of the commander machine.])
# ARG_HELP([Deploy the app in one step])
# ARGBASH_GO()
# needed because of Argbash --> m4_ignore([
### START OF CODE GENERATED BY Argbash v2.4.0 one line above ###
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

begins_with_short_option()
{
	local first_option all_short_options
	all_short_options='beiHh'
	first_option="${1:0:1}"
	test "$all_short_options" = "${all_short_options/$first_option/}" && return 1 || return 0
}



# THE DEFAULTS INITIALIZATION - OPTIONALS
_arg_ansible_branch="master"
_arg_env=
_arg_key_file=
_arg_host=

print_help ()
{
	echo "Deploy the app in one step"
	printf 'Usage: %s [-b|--ansible-branch <arg>] [-e|--env <arg>] [-i|--key-file <arg>] [-H|--host <arg>] [-h|--help]\n' "$0"
	printf "\t%s\n" "-b,--ansible-branch: Specify which branch of the ansible repo should be used. (default: '"master"')"
	printf "\t%s\n" "-e,--env: Name of the environment in which to deploy. (no default)"
	printf "\t%s\n" "-i,--key-file: Location of the key needed to connect to the commander. (no default)"
	printf "\t%s\n" "-H,--host: URL of the commander machine. (no default)"
	printf "\t%s\n" "-h,--help: Prints help"
}

# THE PARSING ITSELF
while test $# -gt 0
do
	_key="$1"
	case "$_key" in
		-b*|--ansible-branch|--ansible-branch=*)
			_val_from_long="${_key##--ansible-branch=}"
			_val_from_short="${_key##-b}"
			if test "$_val_from_long" != "$_key"
			then
				_val="$_val_from_long"
			elif test "$_val_from_short" != "$_key" -a -n "$_val_from_short"
			then
				_val="$_val_from_short"
			else
				test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
				_val="$2"
				shift
			fi
			_arg_ansible_branch="$_val"
			;;
		-e*|--env|--env=*)
			_val_from_long="${_key##--env=}"
			_val_from_short="${_key##-e}"
			if test "$_val_from_long" != "$_key"
			then
				_val="$_val_from_long"
			elif test "$_val_from_short" != "$_key" -a -n "$_val_from_short"
			then
				_val="$_val_from_short"
			else
				test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
				_val="$2"
				shift
			fi
			_arg_env="$_val"
			;;
		-i*|--key-file|--key-file=*)
			_val_from_long="${_key##--key-file=}"
			_val_from_short="${_key##-i}"
			if test "$_val_from_long" != "$_key"
			then
				_val="$_val_from_long"
			elif test "$_val_from_short" != "$_key" -a -n "$_val_from_short"
			then
				_val="$_val_from_short"
			else
				test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
				_val="$2"
				shift
			fi
			_arg_key_file="$_val"
			;;
		-H*|--host|--host=*)
			_val_from_long="${_key##--host=}"
			_val_from_short="${_key##-H}"
			if test "$_val_from_long" != "$_key"
			then
				_val="$_val_from_long"
			elif test "$_val_from_short" != "$_key" -a -n "$_val_from_short"
			then
				_val="$_val_from_short"
			else
				test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
				_val="$2"
				shift
			fi
			_arg_host="$_val"
			;;
		-h*|--help)
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
# [ <-- needed because of Argbash

STOCKMIND_VERSION=$( awk '/^  version :=/ {print $3}' build.sbt | sed -e 's/^"//' -e 's/",$//' )
STOCKMIND_DEB_NAME=stockmind-api_${STOCKMIND_VERSION}_all.deb
STOCKMIND_DEB_PATH=target/${STOCKMIND_DEB_NAME}

if [[ -f ${_arg_key_file} ]]
then

    echo ""
    echo "----------------------------------------"
    echo "Building deb file..."
    echo "----------------------------------------"
    sbt clean debian:packageBin

    echo ""
    echo "----------------------------------------"
    echo "Uploading deb file and deploy instructions to commander..."
    echo "----------------------------------------"

    scp -i ${_arg_key_file} ${STOCKMIND_DEB_PATH} ubuntu@${_arg_host}:~/${STOCKMIND_DEB_NAME}
    scp -i ${_arg_key_file} commander-instructions.sh ubuntu@${_arg_host}:~/commander-instructions.sh

    echo ""
    echo "----------------------------------------"
    echo "Running instructions..."
    echo "----------------------------------------"

    ssh -i ${_arg_key_file} ubuntu@${_arg_host} /bin/bash \
        commander-instructions.sh ${STOCKMIND_DEB_NAME} ${_arg_ansible_branch} ${_arg_env}

else
    echo "EC2 key not present at ${_arg_key_file}"
    echo "Please put the private EC2 key there."
fi

echo ""
echo "----------------------------------------"
echo "Cleaning..."
echo "----------------------------------------"

# ] <-- needed because of Argbash
