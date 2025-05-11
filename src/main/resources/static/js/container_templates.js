// Define container templates as a separate module
const containerTemplates = {
    'ubuntu': {
        image: 'ubuntu:22.04',
        port: 22,
        cmd: 'sleep infinity'
    },

    'nginx': {
        image: 'nginx:latest',
        port: 80,
        env: {
            'NGINX_HOST': 'localhost'
        },
        cmd: ''
    },

    'mysql': {
        image: 'mysql:8.0',
        port: 3306,
        cmd: '',
        env: {
            'MYSQL_ROOT_PASSWORD': 'password',
            'MYSQL_DATABASE': 'mydb'
        }
    },

    'oracle database': {
        image: 'gvenzl/oracle-xe:21',
        port: 1521,
        cmd: '',
        env: {
            'ORACLE_PWD': 'password'
        }
    },
};

export default containerTemplates;