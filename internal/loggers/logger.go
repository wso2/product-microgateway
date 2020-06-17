package loggers

import (
	"github.com/sirupsen/logrus"
	"github.com/wso2/micro-gw/internal/pkg/logging"
)

/* loggers should be initiated only for the main packages
 ********** Don't initiate loggers for sub packages ****************

When you add a new logger instance add the related package name as a constant
 */


const (
	pkgApi = "github.com/wso2/micro-gw/internal/pkg/api"
	pkgAuth = "github.com/wso2/micro-gw/internal/pkg/auth"
	pkgMgw = "github.com/wso2/micro-gw/internal/pkg/mgw"
	pkgOasparser = "github.com/wso2/micro-gw/internal/pkg/oasparser"
)

var (
	LoggerApi          *logrus.Logger
	LoggerAuth         *logrus.Logger
	LoggerMgw          *logrus.Logger
	LoggerOasparser    *logrus.Logger
)

func init() {
	UpdateLoggers()
}

func UpdateLoggers() {
	logrus.Info("Updating loggers....")

	LoggerApi = logging.InitPackageLogger(pkgApi)
	LoggerAuth = logging.InitPackageLogger(pkgAuth)
	LoggerMgw = logging.InitPackageLogger(pkgMgw)
	LoggerOasparser = logging.InitPackageLogger(pkgOasparser)
}
